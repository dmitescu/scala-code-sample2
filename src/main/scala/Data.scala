package com.github.dmitescu

import cats.syntax.functor._

import io.circe._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._

import scala.math.pow

import java.text.SimpleDateFormat
import java.util.Date

object CalculationJSON {
  val dateFormat = new SimpleDateFormat("yyyy-MM-dd");

  implicit val decUf = Decoder[UpfrontFee].widen
  implicit val decUcf = Decoder[UpfrontCreditlineFee].widen
  implicit val dateTimeEncoder: Encoder[Date] =
    Encoder.instance(a => dateFormat.format(a).asJson)
  implicit val dateTimeDecoder: Decoder[Date] =
    Decoder.instance(a => a.as[String].map(dateFormat.parse(_)))

  implicit val decS = Decoder[Schedule].widen
  implicit val dec = Decoder[Calculation].widen

  implicit val decR = Decoder[CalculationResult].widen
}

case class Calculation(
  principal: Integer,
  upfrontFee: UpfrontFee,
  upfrontCreditlineFee: UpfrontCreditlineFee,
  schedule: List[Schedule]
)

case class CalculationResult(
  apr: Double,
  irr: Double)

case class UpfrontFee(value: Integer)
case class UpfrontCreditlineFee(value: Integer)

case class Schedule(id: Integer,
  date: Date,
  principal: Integer,
  interestFee: Integer)

case class Period(segment: Double, cashFlow: Integer)
case class Periods(periods: List[Period])

object IRR {
  def f(i: Periods, x: Double) =
    i.periods.map { p =>
      p.cashFlow.toDouble / pow(x + 1, p.segment)
    }.sum

  def fromCalculation(calc: Calculation): Periods =  {
    val periods = List(Period(0,
      (calc.principal -
        calc.upfrontFee.value -
        calc.upfrontCreditlineFee.value) * -1)) ++
    calc.schedule.map { s =>
      Period(s.id.toDouble, s.principal + s.interestFee) }

    Periods(periods)
  }

  def printInfo(i: Periods) = {
    import cats._
    import cats.implicits._

    val x = (for {
      bounds <- SecantMethod.findBounds(i)
      solution <- SecantMethod.solve(
        i , bounds._1, bounds._2, 0)
    } yield solution).foldMap(SecantMethod.compiler(f))

    println(x)
  }
}

object APR {
  def f(i: Periods, x: Double) =
    i.periods.map { p =>
      p.cashFlow.toDouble / pow(x/100 + 1, p.segment)
    }.sum

  def fromCalculation(calc: Calculation): Periods =  {
    val days = calc.schedule.sliding(2)
      .map { s =>
        (s.tail.head.date.getTime - s.head.date.getTime) / 86400000
      }.toList

    val daysCummulated = days.tail.foldRight(List(days.head)) {
      case (v, a) => a ++ List(a.last + v)
    }

    val periods = List(Period(0,
      (calc.principal -
        calc.upfrontFee.value -
        calc.upfrontCreditlineFee.value) * -1)) ++
    calc.schedule.zip(daysCummulated).map { case (s, d) =>
      Period(d.toDouble / 365.toDouble, s.principal + s.interestFee) }

    Periods(periods)
  }

  def printInfo(i: Periods) = {
    import cats._
    import cats.implicits._

    val x = (for {
      bounds <- SecantMethod.findBounds(i, 100)
      solution <- SecantMethod.solve(
        i , bounds._1, bounds._2, 0)
    } yield solution).foldMap(SecantMethod.compiler(f))

    println(x)
  }
}
