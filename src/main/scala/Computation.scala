package com.github.dmitescu

import cats._
import cats.implicits._

import cats.free._
import cats.free.Free.liftF

import cats.arrow.FunctionK
import cats.{Id, ~>}

import scala.util.Random

case class XN(x: Double)
case class XM(x: Double)

sealed trait SComputation[R]
case class SFindBounds(i: Periods, factor: Double = 1)
    extends SComputation[(XN, XM)]
case class SSolve(i: Periods, a: XN, b: XM, d: BigInt)
    extends SComputation[Double]

object SecantMethod {
  val maxError = Configuration.maxError
  val maxDepth = Configuration.maxDepth
  val boundSteps = Configuration.boundSteps

  val maxDepthExceeded = new Throwable("Max depth exceeded!")
  val nonSense = new Throwable("Nonsense")
  val noBounds = new Throwable("No bounds found")

  type SecantMethodC[R] = Free[SComputation, R]
  type Result[T] = Either[Throwable, T]

  def findBounds(i: Periods, factor: Double = 1): SecantMethodC[(XN, XM)] =
    liftF[SComputation, (XN, XM)](SFindBounds(i, factor))

  def solve(i: Periods, a: XN, b: XM, d: BigInt)
      : SecantMethodC[Double] =
    liftF[SComputation, Double](SSolve(i, a, b, d))

  def compiler(f: (Periods, Double) => Double): SComputation ~> Result = {
    val rng = Random
    new (SComputation ~> Result) {
      def recurse(i: Periods, a: XN, b: XM, d: BigInt)
          : Trampoline[Result[Double]] =
        (d > maxDepth) match {
          case true => Trampoline.done(Left(maxDepthExceeded))
          case false =>
            f(i, a.x).abs < maxError match {
              case true => Trampoline.done(Right(a.x))
              case false => Trampoline.defer {
                val x2 = b.x - f(i, b.x) * (b.x - a.x)/(f(i, b.x) - f(i, a.x) )
                recurse(i, XN(x2), XM(b.x), d + 1)
              }
            }
        }

      def apply[A](c: SComputation[A]): Result[A]  =
        c match {
          case SFindBounds(i: Periods, factor: Double) =>
            i.periods.foldRight(0) {
              case (p, a) => p.cashFlow + a } <= 0 ||
              i.periods.length < 2 match {
              case true => Left(nonSense)
                case false =>
                  val f0 = f(i, 0).signum

                  val B = (1 to boundSteps)
                    .toList
                    .map(_ => rng.nextDouble * factor)
                    .filter(b => f(i, b) * f0 < 0)
                    .minimumOption

                  val o = B.map { BVal =>
                    (1 to boundSteps)
                      .toList.sliding(2)
                      .map { j =>
                        val x1 = j.head * BVal/boundSteps
                        val x2 = j.tail.head * BVal/boundSteps
                        val fx1 = f(i, x1)
                        val fx2 = f(i, x2)
                        (fx1 < 0 && fx2 > 0) || (fx1 > 0 && fx2 < 0) match {
                          case false => None
                          case true => Some((x1, x2))
                        }
                      }.toList.flatten.headOption
                  }.flatten
                  
                  o match {
                    case Some((a, b)) => Right(XN(a), XM(b))
                    case None => Left(noBounds)
                  }
            }
          case SSolve(i: Periods, a: XN, b: XM, d: BigInt) =>
            recurse(i, a, b, d).run
        }
    }
  }
}
