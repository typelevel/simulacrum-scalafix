package example

object TestLambdaParam {
  val xs = List(1, 2, 3).map { (i: Int) =>
    i + 1
  }
  val ys = List(1, 2, 3).map { i =>
    i + 1
  }
  val zs = List(1, 2, 3).map { (i: Int) =>
    i + 1
  }
}