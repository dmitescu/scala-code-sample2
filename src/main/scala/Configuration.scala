package com.github.dmitescu

object Configuration {
  def maxError = sys.env.get("SPOTCAP_MAXERROR").map(_.toInt).getOrElse(5)
  def maxDepth = sys.env.get("SPOTCAP_MAXDEPTH").map(_.toInt).getOrElse(50000)
  def boundSteps = sys.env.get("SPOTCAP_BOUND_STEPS").map(_.toInt).getOrElse(10)
}
