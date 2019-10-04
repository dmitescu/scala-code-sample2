package com.github.dmitescu

import cats.implicits._

import org.specs2.matcher._
import org.specs2.mutable.Specification

import java.text.SimpleDateFormat

import scala.math.pow

class DataSpec extends Specification {
  val dateFormat = new SimpleDateFormat("yyyy-MM-dd");

  val calc = Calculation(
    1234600,
    UpfrontFee(100),
    UpfrontCreditlineFee(0),
    List(
      Schedule(1, dateFormat.parse("2018-01-01"), 361000, 1000),
      Schedule(2, dateFormat.parse("2018-02-01"), 546000, 2000),
      Schedule(3, dateFormat.parse("2018-03-01"), 478000, 3000)
    ))

  val aprPeriods = Periods({
    val raw = List(
      Period(0, -1234500),
      Period(0.08493150684931507, 362000)
    )

    raw.tail.foldRight(List(raw.head)) {
      case (p, a) => a ++ List(Period(a.last.segment + p.segment, p.cashFlow))
    }
  })

  val irrPeriods = Periods(List(
    Period(0, -1234500),
    Period(1, 362000),
    Period(2, 548000),
    Period(3, 481000)
  ))

  "Data should" >> {
    "encode APRs correctly" >> {
      APR.fromCalculation(calc).periods must contain(allOf(aprPeriods.periods: _*))
    }

    "encode IRRs correctly" >> {
      IRR.fromCalculation(calc).periods must contain(allOf(irrPeriods.periods: _*))
    }
  }
}
