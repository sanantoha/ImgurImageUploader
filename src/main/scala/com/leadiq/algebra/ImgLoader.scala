package com.leadiq.algebra

trait ImgLoader[F[_]] {

  def load(url: String): F[Array[Byte]]
}
