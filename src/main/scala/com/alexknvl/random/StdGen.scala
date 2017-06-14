package com.alexknvl.random

import java.nio.ByteBuffer

import scala.annotation.tailrec

object StdGen {
  final val GOLDEN_GAMMA: Long = 0x9e3779b97f4a7c15L

  private final def mix64(z: Long): Long = {
    val z1 = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L
    val z2 = (z1 ^ (z1 >>> 27)) * 0x94d049bb133111ebL
    z2 ^ (z2 >>> 31)
  }

  private final def mix32(z: Long): Int = {
    val z1 = (z ^ (z >>> 33)) * 0x62a9d9ed799705f5L
    (((z1 ^ (z1 >>> 28)) * 0xcb24d0a5c88c35b3L) >>> 32).toInt
  }

  private def mixGamma(z: Long): Long = {
    // MurmurHash3 mix constants
    val z1 = (z ^ (z >>> 33)) * 0xff51afd7ed558ccdL
    val z2 = (z1 ^ (z1 >>> 33)) * 0xc4ceb9fe1a85ec53L
    // force to be odd
    val z3 = (z2 ^ (z2 >>> 33)) | 1L
    // ensure enough transitions
    val n: Int = java.lang.Long.bitCount(z3 ^ (z3 >>> 1))
    if (n < 24) z3 ^ 0xaaaaaaaaaaaaaaaaL
    else z3
  }

  implicit val randomGen: RandomGen[StdGen] = new RandomGen[StdGen] {
    override def seed(array: Array[Byte]): StdGen = {
      val bb: ByteBuffer = ByteBuffer.wrap(array)
      @tailrec def go(i: Int, h: Long): Long = if (i >= array.length) h else {
        if (i + 8 <= array.length) go(i + 8, h ^ mix64(bb.getLong(i)))
        else if (i + 4 <= array.length) go(i + 4, h ^ mix64(bb.getInt(i).toLong))
        else if (i + 2 <= array.length) go(i + 2, h ^ mix64(bb.getShort(i).toLong))
        else go(i + 1, h ^ mix64(bb.get(i).toLong))
      }
      val s = go(0, 0L)
      StdGen(s, mixGamma(s + GOLDEN_GAMMA))
    }
    override def nextLong(g: StdGen): (Long, StdGen) = g.next64
    override def nextInt(g: StdGen): (Int, StdGen) = g.next32

    override def fork(g: StdGen): (StdGen, StdGen) = g.fork
    override def join(g1: StdGen, g2: StdGen): StdGen = g1.join(g2)
  }
}
final case class StdGen(seed: Long, gamma: Long) {
  import StdGen._

  def next64: (Long, StdGen) = {
    val s = seed + gamma
    (mix64(s), StdGen(s, gamma))
  }

  def next32: (Int, StdGen) = {
    val s = seed + gamma
    (mix32(s), StdGen(s, gamma))
  }

  def fork: (StdGen, StdGen) = {
    val s = seed + gamma
    (StdGen(s, gamma), StdGen(mix64(s), mixGamma(s)))
  }

  // Honestly, I have no idea what I am doing here ;)
  def join(that: StdGen): StdGen =
    StdGen(
      mix64(this.seed) ^ mix64(that.seed),
      mixGamma(mix64(this.gamma) ^ mix64(that.gamma)))
}
