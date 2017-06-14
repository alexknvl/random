package com.alexknvl.random

import scala.annotation.unchecked.{uncheckedVariance => uV}
import cats.free.Free
import cats.{Id, Monad, ~>}

sealed abstract class Gen[+A] { self =>
  import Gen._

  private[random] def free: Free[PrimitiveGen, A @uV]

  def apply[G](g: G)(implicit G: RandomGen[G]): (A, G) = {
    var gen: G = g
    val a = free.foldMap(new (PrimitiveGen ~> Id) {
      override def apply[B](fa: PrimitiveGen[B]): Id[B] = {
        val (b, g1) = fa.apply(gen)(G)
        gen = g1
        b
      }
    })
    (a, gen)
  }

  def map[B](f: A => B): Gen[B] =
    new Gen[B] { val free = self.free.map(f) }
  def flatMap[B](f: A => Gen[B]): Gen[B] =
    new Gen[B] { val free = self.free.flatMap(f andThen (_.free)) }
}
object Gen {
  private[random] sealed abstract class PrimitiveGen[+A] extends Gen[A] {
    val free: Free[PrimitiveGen, A @uV] = Free.liftF(this)
    def apply[G](g: G)(implicit G: RandomGen[G]): (A, G)
  }

  def pure[A](a: A): Gen[A] = new PrimitiveGen[A] {
    override def apply[G](g: G)(implicit G: RandomGen[G]): (A, G) = (a, g)
  }

  def byte: Gen[Byte] = new PrimitiveGen[Byte] {
    override def apply[G](g: G)(implicit G: RandomGen[G]): (Byte, G) = G.nextByte(g)
  }

  def short: Gen[Short] = new PrimitiveGen[Short] {
    override def apply[G](g: G)(implicit G: RandomGen[G]): (Short, G) = G.nextShort(g)
  }

  def int: Gen[Int] = new PrimitiveGen[Int] {
    override def apply[G](g: G)(implicit G: RandomGen[G]): (Int, G) = G.nextInt(g)
  }
  def boundedInt(bound: Int): Gen[Int] = new PrimitiveGen[Int] {
    override def apply[G](g: G)(implicit G: RandomGen[G]): (Int, G) = G.nextInt1(g, bound)
  }

  def choose[A](seq: IndexedSeq[A]): Gen[A] = new PrimitiveGen[A] {
    override def apply[G](g: G)(implicit G: RandomGen[G]): (A, G) = {
      val (i, g1) = G.nextInt1(g, seq.size)
      (seq(i), g1)
    }
  }

  def long: Gen[Long] = new PrimitiveGen[Long] {
    override def apply[G](g: G)(implicit G: RandomGen[G]): (Long, G) = G.nextLong(g)
  }
  def boundedLong(bound: Long): Gen[Long] = new PrimitiveGen[Long] {
    override def apply[G](g: G)(implicit G: RandomGen[G]): (Long, G) = G.nextLong1(g, bound)
  }

  implicit val monad: Monad[Gen] = new Monad[Gen] with Random[Gen] {
    override def withGen[A](g: Gen[A]): Gen[A] = g

    override def pure[A](x: A): Gen[A] = Gen.pure(x)
    override def map[A, B](fa: Gen[A])(f: A => B): Gen[B] = fa.map(f)
    override def flatMap[A, B](fa: Gen[A])(f: A => Gen[B]): Gen[B] = fa.flatMap(f)

    override def tailRecM[A, B](a: A)(f: (A) => Gen[Either[A, B]]): Gen[B] =
      f(a).flatMap {
        case Left(x) => tailRecM(x)(f)
        case Right(x) => Gen.pure(x)
      }
  }
}