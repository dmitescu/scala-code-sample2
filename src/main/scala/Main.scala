package com.github.dmitescu

import cats._
import cats.implicits._
import cats.syntax.functor._

import cats.effect.{ContextShift, IO}

import io.finch._
import io.finch.circe._
import io.finch.syntax._

import com.twitter.finagle._
import com.twitter.util.Await

import com.twitter.finagle.{Http, Service}
import com.twitter.finagle.http.{Request, Response}

import io.circe._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._

import org.log4s._
import scala.math.BigDecimal

object Main extends App {
  import CalculationJSON._
  import cats._
  import cats.implicits._

  private[this] val logger = getLogger

  val compute: Endpoint[CalculationResult] =
    post("compute" :: jsonBody[Calculation]) { c: Calculation =>
      val cr = for {
        aprPeriods <- Right(APR.fromCalculation(c))
        irrPeriods <- Right(IRR.fromCalculation(c))

        apr <- (for {
          bounds <- SecantMethod.findBounds(aprPeriods, 100)
          result <- SecantMethod.solve(aprPeriods, bounds._1, bounds._2, 0)
        } yield result).foldMap(SecantMethod.compiler(APR.f))

        irr <- (for {
          bounds <- SecantMethod.findBounds(irrPeriods)
          result <- SecantMethod.solve(irrPeriods, bounds._1, bounds._2, 0)
        } yield result).foldMap(SecantMethod.compiler(IRR.f))

        aprRep = BigDecimal(apr)
        .setScale(2, BigDecimal.RoundingMode.FLOOR)
        .toDouble
        irrRep = BigDecimal(irr)
        .setScale(10, BigDecimal.RoundingMode.FLOOR)
        .toDouble
      } yield CalculationResult(aprRep, irrRep)

      cr.fold(
        t => logger.warn(s"Error: ${t}"),
        c => logger.info(s"Served: ${c.toString}")
      )

      cr.fold(
        t => InternalServerError(new Exception(t)),
        c => Ok(c)
      )
    }

  Await.ready(Http.server.serve(":8080", compute.toService))
}
