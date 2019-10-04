package com.github.dmitescu

import cats.implicits._

import org.specs2.matcher._
import org.specs2.mutable.Specification

import scala.math.pow

class ComputationSpec extends Specification {
  val periods = Periods(List(
    Period(0, -1234500),
    Period(1, 362000),
    Period(2, 548000),
    Period(3, 481000)
  ))

  val predefinedBound: Double = 0.1

  def f(i: Periods, x: Double) = i.periods.map { p =>
      p.cashFlow.toDouble / pow(x + 1, p.segment)
    }.sum


  "SecantMethod should" >> {
    "find a bound 0 < f(x) < b" >> {
      SecantMethod
        .findBounds(periods)
        .foldMap(SecantMethod.compiler(f)) must beRight
    }

    "compute the right value" >> {
      SecantMethod
        .solve(periods, XN(0), XM(predefinedBound), 0)
        .foldMap(SecantMethod.compiler(f)) must
      beRight(beBetween(0.059, 0.0599))
    }

    "fail on ambiguous input (i.e: payment too big)" >> {
      SecantMethod
        .solve(Periods(periods.periods.map {
          p => p.cashFlow > 0 match {
            case true => p
            case false => Period(p.segment, p.cashFlow - p.cashFlow - 1)
          }
        }), XN(0), XM(predefinedBound), 0)
        .foldMap(SecantMethod.compiler(f)) must beLeft
    }

    "fail on wrong bound" >> {
      SecantMethod
        .solve(periods, XN(10), XM(20), 0)
        .foldMap(SecantMethod.compiler(f)) must beLeft
    }
  }
}
