package com.leadiq.algebra

trait KVStore[F[_], V] {
  def get(key: String): F[Option[V]]
  def put(key: String, value: V): F[Unit]
  def update(key: String, f: V => V): F[Unit]
  def values(): F[List[V]]
}
