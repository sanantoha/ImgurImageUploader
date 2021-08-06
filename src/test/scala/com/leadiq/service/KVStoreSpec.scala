package com.leadiq.service

import cats.effect.{Sync, SyncIO}
import cats.syntax.option._
import com.leadiq.UnitSpec
import com.leadiq.algebra.KVStore
import org.scalatest.BeforeAndAfter

import scala.collection.concurrent.TrieMap

class KVStoreSpec extends UnitSpec with BeforeAndAfter {

  val key1 = "key1"
  val value1 = "value1"


  "KVStore" should "return value on get" in {
    mkKVStore[SyncIO]().get(key1).unsafeRunSync() shouldBe value1.some
  }

  it should "return none if value is not contains" in {
    mkKVStore[SyncIO]().get("key10").unsafeRunSync() shouldBe none
  }

  it should "save value" in {
    val expKey = "key10"
    val expValue = "value10"
    val (state, store) = mkKVStoreWithState[SyncIO]()
    store.put(expKey, expValue).unsafeRunSync()

    state.get(expKey) shouldBe expValue.some
  }

  it should "return all values" in {
    mkKVStore[SyncIO]().values().unsafeRunSync() shouldBe List("value1", "value2")
  }

  it should "update value" in {
    val (state, store) = mkKVStoreWithState[SyncIO]()

    store.update(key1, _ + "!!!").unsafeRunSync()

    state.get(key1) shouldBe "value1!!!".some
  }

  def mkKVStoreWithState[F[_]: Sync](): (TrieMap[String, String], KVStore[F, String]) = {
    val init = TrieMap[String, String](key1 -> value1, "key2" -> "value2")
    (init, new KVStoreImpl[F, String](init))
  }

  def mkKVStore[F[_]: Sync](): KVStore[F, String] = mkKVStoreWithState[F]()._2
}