package com.leadiq.algebra

import java.time.LocalDateTime

trait DateTime[F[_]] {

  def now: F[LocalDateTime]
}
