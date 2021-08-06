package com.leadiq.algebra

trait JobTaskScheduler[F[_]] {

  def run(): F[Unit]
}
