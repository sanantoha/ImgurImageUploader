package com.leadiq

import org.scalatest._

abstract class UnitSpec
  extends FlatSpec
    with Matchers
    with OptionValues
    with EitherValues
    with Inside
    with Inspectors