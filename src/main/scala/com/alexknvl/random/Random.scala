package com.alexknvl.random

trait Random[F[_]] {
  def withGen[A](g: Gen[A]): F[A]

  def randomInt: F[Int] = withGen(Gen.int)
  def randomLong: F[Long] = withGen(Gen.long)
}