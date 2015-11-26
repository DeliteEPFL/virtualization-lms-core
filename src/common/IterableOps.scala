package scala.lms
package common

import java.io.PrintWriter
import internal._
import scala.reflect.SourceContext

trait IterableOps extends Variables {

  implicit def iterableTyp[T:Typ]: Typ[Iterable[T]]

  // multiple definitions needed because implicits won't chain
  // not using infix here because apply doesn't work with infix methods
  implicit def varToIterableOps[A:Typ](x: Var[Iterable[A]]) = new IterableOpsCls(readVar(x))
  implicit def repIterableToIterableOps[T:Typ](a: Rep[Iterable[T]]) = new IterableOpsCls(a)
  implicit def iterableToIterableOps[T:Typ](a: Iterable[T]) = new IterableOpsCls(unit(a))

  class IterableOpsCls[T:Typ](a: Rep[Iterable[T]]){
    def foreach(block: Rep[T] => Rep[Unit])(implicit pos: SourceContext) = iterable_foreach(a, block)
    def toArray(implicit pos: SourceContext) = iterable_toarray(a)
  }

  def iterable_foreach[T:Typ](x: Rep[Iterable[T]], block: Rep[T] => Rep[Unit])(implicit pos: SourceContext): Rep[Unit]
  def iterable_toarray[T:Typ](x: Rep[Iterable[T]])(implicit pos: SourceContext): Rep[Array[T]]
}

trait IterableOpsExp extends IterableOps with EffectExp with VariablesExp with ArrayOpsExp {

  implicit def iterableTyp[T:Typ]: Typ[Iterable[T]] = {
    implicit val ManifestTyp(m) = typ[T]
    manifestTyp
  }

  case class IterableForeach[T:Typ](a: Exp[Iterable[T]], x: Sym[T], block: Block[Unit]) extends Def[Unit]
  case class IterableToArray[T:Typ](a: Exp[Iterable[T]]) extends Def[Array[T]] {
    val m = manifest[T]
  }
  
  def iterable_foreach[T:Typ](a: Exp[Iterable[T]], block: Exp[T] => Exp[Unit])(implicit pos: SourceContext): Exp[Unit] = {
    val x = fresh[T]
    val b = reifyEffects(block(x))
    reflectEffect(IterableForeach(a, x, b), summarizeEffects(b).star)
  }
  def iterable_toarray[T:Typ](a: Exp[Iterable[T]])(implicit pos: SourceContext) = IterableToArray(a)

  override def mirror[A:Typ](e: Def[A], f: Transformer)(implicit pos: SourceContext): Exp[A] = {
    (e match {
      case e@IterableToArray(x) => iterable_toarray(f(x))(e.m,pos)
      case Reflect(e@IterableForeach(x,y,b), u, es) => reflectMirrored(Reflect(IterableForeach(f(x),f(y).asInstanceOf[Sym[_]],f(b))(anyTyp), mapOver(f,u), f(es)))(mtype(manifest[A]), pos)
      case Reflect(e@IterableToArray(x), u, es) => reflectMirrored(Reflect(IterableToArray(f(x))(e.m), mapOver(f,u), f(es)))(mtype(manifest[A]), pos)    
      case _ => super.mirror(e,f)
    }).asInstanceOf[Exp[A]] // why??
  }
  
  override def syms(e: Any): List[Sym[Any]] = e match {
    case IterableForeach(a, x, body) => syms(a):::syms(body)
    case _ => super.syms(e)
  }

  override def boundSyms(e: Any): List[Sym[Any]] = e match {
    case IterableForeach(a, x, body) => x :: effectSyms(body)
    case _ => super.boundSyms(e)
  }

  override def symsFreq(e: Any): List[(Sym[Any], Double)] = e match {
    case IterableForeach(a, x, body) => freqNormal(a):::freqHot(body)
    case _ => super.symsFreq(e)
  }
}

trait BaseGenIterableOps extends GenericNestedCodegen {
  val IR: IterableOpsExp
  import IR._

}

trait ScalaGenIterableOps extends BaseGenIterableOps with ScalaGenBase {
  val IR: IterableOpsExp
  import IR._

  override def emitNode(sym: Sym[Any], rhs: Def[Any]) = rhs match {
    case IterableForeach(a,x,block) =>
      gen"""val $sym=$a.foreach{
            |$x =>
            |${nestedBlock(block)}
            |$block
            |}"""
    case IterableToArray(a) => emitValDef(sym, src"$a.toArray")
    case _ => super.emitNode(sym, rhs)
  }
}

trait CLikeGenIterableOps extends BaseGenIterableOps with CLikeGenBase {
  val IR: IterableOpsExp
  import IR._

  override def emitNode(sym: Sym[Any], rhs: Def[Any]) = {
      rhs match {
        case _ => super.emitNode(sym, rhs)
      }
    }
}

trait CudaGenIterableOps extends CudaGenBase with CLikeGenIterableOps
trait OpenCLGenIterableOps extends OpenCLGenBase with CLikeGenIterableOps
trait CGenIterableOps extends CGenBase with CLikeGenIterableOps

