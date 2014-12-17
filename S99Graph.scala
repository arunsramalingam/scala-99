abstract class GraphBase[T, U] {
  case class Edge(n1: Node, n2: Node, value: U) {
    def toTuple = (n1.value, n2.value, value)
  }
  case class Node(value: T) {
    var adj: List[Edge] = Nil
    // neighbors are all nodes adjacent to this node.
    def neighbors: List[Node] = adj.map(edgeTarget(_, this).get)
  }

  var nodes: Map[T, Node] = Map()
  var edges: List[Edge] = Nil
  
  // If the edge E connects N to another node, returns the other node,
  // otherwise returns None.
  def edgeTarget(e: Edge, n: Node): Option[Node]

  override def equals(o: Any) = o match {
    case g: GraphBase[_,_] => ((nodes.keys.toList diff g.nodes.keys.toList) == Nil &&
                               (edges.map(_.toTuple) diff g.edges.map(_.toTuple)) == Nil)
    case _ => false
  }
  def addNode(value: T) = {
    val n = new Node(value)
    nodes = Map(value -> n) ++ nodes
    n
  }

  def toTermForm: (List[T], List[(T, T, U)]) = {
    (nodes.keys.toList, edges.map(x => (x.n1.value, x.n2.value, x.value)))
  }

  def toAdjacentForm: List[(T, List[(T, U)])] = {
    nodes.keys.toList.map(x => {
      (x, edges.filter(_.n1.value == x).map(y => (y.n2.value, y.value)) )
    })
  }

  def findPaths(a: T, b: T): List[List[T]] = {
    //@annotation.tailrec
    def go(c: T, acc: List[T]): List[List[T]] = {
      if (b == c) List(c :: acc)
      else edges.filter(e => e.n1.value == c && !acc.contains(e.n2.value)).flatMap(e => go(e.n2.value, c :: acc))
    }
    go(a, Nil).map(_.reverse)
  }
}

class Graph[T, U] extends GraphBase[T, U] {
  override def equals(o: Any) = o match {
    case g: Graph[_,_] => super.equals(g)
    case _ => false
  }

  def edgeTarget(e: Edge, n: Node): Option[Node] =
    if (e.n1 == n) Some(e.n2)
    else if (e.n2 == n) Some(e.n1)
    else None

  def addEdge(n1: T, n2: T, value: U) = {
    val e = new Edge(nodes(n1), nodes(n2), value)
    edges = e :: edges
    nodes(n1).adj = e :: nodes(n1).adj
    nodes(n2).adj = e :: nodes(n2).adj
  }
}

class Digraph[T, U] extends GraphBase[T, U] {
  override def equals(o: Any) = o match {
    case g: Digraph[_,_] => super.equals(g)
    case _ => false
  }

  def edgeTarget(e: Edge, n: Node): Option[Node] =
    if (e.n1 == n) Some(e.n2)
    else None

  def addArc(source: T, dest: T, value: U) = {
    val e = new Edge(nodes(source), nodes(dest), value)
    edges = e :: edges
    nodes(source).adj = e :: nodes(source).adj
  }
}

abstract class GraphObjBase {
  type GraphClass[T, U]
  def addLabel[T](edges: List[(T, T)]) =
    edges.map(v => (v._1, v._2, ()))
  def term[T](nodes: List[T], edges: List[(T,T)]) =
    termLabel(nodes, addLabel(edges))
  def termLabel[T, U](nodes: List[T], edges: List[(T,T,U)]): GraphClass[T, U]
  def addAdjacentLabel[T](nodes: List[(T, List[T])]) =
    nodes.map(a => (a._1, a._2.map((_, ()))))
  def adjacent[T](nodes: List[(T, List[T])]) =
    adjacentLabel(addAdjacentLabel(nodes))
  def adjacentLabel[T, U](nodes: List[(T, List[(T,U)])]): GraphClass[T, U]
}

object Graph extends GraphObjBase {
  type GraphClass[T, U] = Graph[T, U]

  def termLabel[T, U](nodes: List[T], edges: List[(T,T,U)]) = {
    val g = new Graph[T, U]
    nodes.map(g.addNode)
    edges.map(v => g.addEdge(v._1, v._2, v._3))
    g
  }
  def adjacentLabel[T, U](nodes: List[(T, List[(T,U)])]) = {
    val g = new Graph[T, U]
    for ((v, a) <- nodes) g.addNode(v)
    for ((n1, a) <- nodes; (n2, l) <- a) {
      if (!g.nodes(n1).neighbors.contains(g.nodes(n2)))
        g.addEdge(n1, n2, l)
    }
    g
  }

  def fromString(s: String): Graph[Char,Unit] = {
    val s1 = s.substring(1,s.length-1).split(",").toList.map(_.trim)
    val nodes = s1.flatMap(_.split('-')).map(_(0)).distinct
    val edges = s1.filter(_.indexOf('-') > 0).map(x => {
      val y = x.split('-')
      (y(0)(0), y(1)(0), ())
    })
    termLabel(nodes,edges)
  }
}

object Digraph extends GraphObjBase {
  type GraphClass[T, U] = Digraph[T, U]

  def termLabel[T, U](nodes: List[T], edges: List[(T,T,U)]) = {
    val g = new Digraph[T, U]
    nodes.map(g.addNode)
    edges.map(v => g.addArc(v._1, v._2, v._3))
    g
  }
  def adjacentLabel[T, U](nodes: List[(T, List[(T,U)])]) = {
    val g = new Digraph[T, U]
    for ((n, a) <- nodes) g.addNode(n)
    for ((s, a) <- nodes; (d, l) <- a) g.addArc(s, d, l)
    g
  }

  def fromStringLabel(s: String): Digraph[Char,Int] = {
    val s1 = s.substring(1,s.length-1).split(",").toList.map(_.trim)
    val nodes = s1.map(_.split('/')(0)).flatMap(_.split('>')).map(_(0)).distinct
    val edges = s1.filter(_.indexOf('/')>0).map(x => {
      val y=x.split('/')
      y.flatMap(_.split('>'))
    }).map(z => (z(0)(0), z(1)(0), z(2).toInt))
    termLabel(nodes,edges)
  }
}

val termform = Graph.term(List('b', 'c', 'd', 'f', 'g', 'h', 'k'),
                          List(('b', 'c'), ('b', 'f'), ('c', 'f'), ('f', 'k'), ('g', 'h'))).toTermForm
val gstring = Graph.fromString("[b-c, f-c, g-h, d, f-b, k-f, h-g]").toTermForm
//res0: (List[String], List[(String, String, Unit)]) = (List(d, k, h, c, f, g, b),List((h,g,()), (k,f,()), (f,b,()), (g,h,()), (f,c,()), (b,c,())))
val adjcform = Digraph.termLabel(List('b', 'c', 'd', 'f', 'g', 'h', 'k'),
               List(('b', 'c', 1), ('b', 'f', 2), ('c', 'f', 3), ('f', 'k', 4), ('g', 'h', 5))).toAdjacentForm
val dstring = Digraph.fromStringLabel("[p>q/9, m>q/7, k, p>m/5]").toAdjacentForm
//res1: List[(String, List[(String, Int)])] = List((m,List((q,7))), (p,List((m,5), (q,9))), (k,List()), (q,List()))
val paths1 = Digraph.fromStringLabel("[p>q/9, m>q/7, k, p>m/5]").findPaths('p', 'q')
//res0: List[List[String]] = List(List(p, q), List(p, m, q))