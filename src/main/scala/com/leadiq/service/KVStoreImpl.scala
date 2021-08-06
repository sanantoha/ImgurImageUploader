package com.leadiq.service

import cats.data.OptionT
import cats.effect.Sync
import cats.syntax.functor._
import cats.syntax.option._
import com.leadiq.algebra.KVStore

import scala.collection.concurrent.TrieMap


class KVStoreImpl[F[_]: Sync, V](storage: TrieMap[String, V]) extends KVStore[F, V] {

  override def get(key: String): F[Option[V]] =
    Sync[F].delay(storage.get(key))

  override def put(key: String, value: V): F[Unit] =
    Sync[F].delay(storage.put(key, value)).void

  override def update(key: String, f: V => V): F[Unit] = tryToUpdate(key, f).void

  override def values(): F[List[V]] = Sync[F].delay(storage.values.toList)

  private def tryToUpdate(key: String, f: V => V) : F[Option[V]] = {
    val res = for {
      oldValue <- OptionT(get(key))
      newValue = f(oldValue)
      value <- if (storage.replace(key, oldValue, newValue)) OptionT.fromOption[F](newValue.some)
               else OptionT(tryToUpdate(key, f))
    } yield value

    res.value
  }

}