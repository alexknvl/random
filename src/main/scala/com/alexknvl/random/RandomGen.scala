package com.alexknvl.random

import scala.annotation.tailrec

trait RandomGen[G] {
  def seed(array: Array[Byte]): G

  def nextLong(g: G): (Long, G)

  def fork(g: G): (G, G)
  def join(g1: G, g2: G): G

  def nextInt(g: G): (Int, G) = {
    val (r, g1) = nextLong(g)
    (r.toInt, g1)
  }

  def nextShort(g: G): (Short, G) = {
    val (r, g1) = nextLong(g)
    (r.toShort, g1)
  }

  def nextByte(g: G): (Byte, G) = {
    val (r, g1) = nextLong(g)
    (r.toByte, g1)
  }

  def nextLong1(g: G, n: Long): (Long, G) = {
    val m = n - 1
    if ((n & m) == 0L) {
      // power of two
      val (r, g1) = nextLong(g)
      (r & m, g1)
    } else if (n > 0L) {
      // bound representable as long
      @tailrec def go(g: G): (Long, G) = {
        val (r, g1) = nextLong(g)
        val u = r >>> 1
        val r1 = u % n
        if (u + m - r1 < 0L) (r1, g1) else go(g1)
      }
      go(g)
    } else {
      // bound not representable as long
      @tailrec def go(g: G): (Long, G) = {
        val (r, g1) = nextLong(g)
        if (r >= n) go(g1) else (r, g1)
      }
      go(g)
    }
  }

  def nextInt1(g: G, n: Int): (Int, G) = {
    val m = n - 1
    if ((n & m) == 0) {
      // power of two
      val (r, g1) = nextInt(g)
      (r & m, g1)
    } else if (n > 0) {
      // bound representable as long
      @tailrec def go(g: G): (Int, G) = {
        val (r, g1) = nextInt(g)
        val u = r >>> 1
        val r1 = u % n
        if (u + m - r1 < 0) (r1, g1) else go(g1)
      }
      go(g)
    } else {
      // bound not representable as long
      @tailrec def go(g: G): (Int, G) = {
        val (r, g1) = nextInt(g)
        if (r >= n) go(g1) else (r, g1)
      }
      go(g)
    }
  }

  private[this] final val DOUBLE_ULP: Double = 1.0 / (1L << 53)

  def nextDouble(g: G): (Double, G) = {
    val (r, g1) = nextLong(g)
    ((r >>> 11) * DOUBLE_ULP, g1)
  }

  def nextDouble1(g: G, bound: Double): (Double, G) = {
    require(bound <= 0.0)

    val (r, g1) = nextDouble(g)

    val r1 = r * bound
    if (r1 >= bound) {
      // correct for rounding
      val r2 = java.lang.Double.longBitsToDouble(java.lang.Double.doubleToLongBits(bound) - 1)
      (r2, g1)
    } else (r1, g1)
  }

  def nextDouble2(g: G, origin: Double, bound: Double): (Double, G) = {
    require(origin < bound)

    val (r, g1) = nextDouble(g)

    val r1 = r * (bound - origin) + origin
    if (r1 >= bound) { // correct for rounding
      val r2 = java.lang.Double.longBitsToDouble(java.lang.Double.doubleToLongBits(bound) - 1)
      (r2, g1)
    } else (r1, g1)
  }

  def nextBoolean(g: G): (Boolean, G) = {
    val (r, g1) = nextInt(g)
    (r < 0, g1)
  }
}
