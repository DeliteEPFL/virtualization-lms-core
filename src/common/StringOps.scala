package scala.virtualization.lms
package common

import java.io.PrintWriter
import scala.virtualization.lms.util.OverloadHack
import scala.virtualization.lms.internal.{GenerationFailedException}
import org.scala_lang.virtualized.SourceContext

trait LiftString {
  this: Base =>

  implicit def strToRepStr(s: String) = unit(s)
}

trait StringOps extends Variables with OverloadHack {
  // NOTE: if something doesn't get lifted, this won't give you a compile time error,
  //       since string concat is defined on all objects

  // TODO(trans) need to check if string concat works

  implicit def repToStringOpsRepTCls[T:Manifest](x: Rep[T])/*(implicit __pos: SourceContext)*/ = new StringOpsRepT(x)//(__pos)
  implicit def liftToStringOpsRepTCls[T:Manifest](x: T)/*(implicit __pos: SourceContext)*/ = new StringOpsRepT(unit(x))//(__pos)
  implicit def varToStringOpsRepTCls[T:Manifest](x: Var[T])/*(implicit __pos: SourceContext)*/ = new StringOpsRepT(readVar(x))//(__pos)

  class StringOpsRepT[T:Manifest](val s1:Rep[T]) {
    def +(s2: Rep[String])(implicit o: Overloaded5, pos: SourceContext) = string_plus(s1, s2)
    def +(s2: Var[String])(implicit o: Overloaded6, pos: SourceContext) = string_plus(s1, readVar(s2))
    def +(s2: String)(implicit o: Overloaded7, pos: SourceContext) = string_plus(s1, unit(s2))
  }

  //TODO(macrotrans): these will not be overloaded by macro trans but have to be taken care of by scala virtualized
  def infix_+(s1: String, s2: Rep[Any])(implicit o: Overloaded1, pos: SourceContext) = string_plus(unit(s1), s2)
  def infix_+[T:Manifest](s1: String, s2: Var[T])(implicit o: Overloaded2, pos: SourceContext) = string_plus(unit(s1), readVar(s2))
  //can this one be inferred? scala-virtualized doesn't know about Rep nor Var...
  def infix_+[T:Manifest](s1: Rep[String], s2: Var[T])(implicit o: Overloaded2, pos: SourceContext) = string_plus(s1, readVar(s2))

  implicit def repToStringOpsCls(x: Rep[String])/*(implicit __pos: SourceContext)*/ = new StringOpsInfixRepString(x)//(__pos)
  //don't lift String otherwise we clash with build-in no-arguments pimpops such as "def toInt"
  //implicit def liftToStringOpsCls(x: String)/*(implicit __pos: SourceContext)*/ = new StringOpsInfixRepString(unit(x))//(__pos)
  implicit def varToStringOpsCls(x: Var[String])/*(implicit __pos: SourceContext)*/ = new StringOpsInfixRepString(readVar(x))//(__pos)
  
  class StringOpsInfixRepString(val s1: Rep[String]) {
    def startsWith(s2: Rep[String])(implicit pos: SourceContext) = string_startswith(s1,s2)
    def trim(separators: Rep[String])(implicit pos: SourceContext) = string_split(s1, separators, unit(0))
    def split(separators: Rep[String])(implicit pos: SourceContext) = string_split(s1, separators)
    def split(separators: Rep[String], limit: Rep[Int])(implicit pos: SourceContext) = string_split(s1, separators, limit)
    def charAt(i: Rep[Int])(implicit pos: SourceContext) = string_charAt(s1,i)
    def endsWith(e: Rep[String])(implicit pos: SourceContext) = string_endsWith(s1,e)
    def contains(s2: Rep[String])(implicit pos: SourceContext) = string_contains(s1,s2)
    def toDouble(start: Rep[Int], end: Rep[Int])(implicit pos: SourceContext) = string_todouble(s1)
    def toInt(implicit pos: SourceContext) = string_toint(s1)
    def substring(start:Rep[Int], end:Rep[Int])(implicit pos: SourceContext) = string_substring(s1, start, end)
    // TODO(trans) check if FIXME still valid
    // FIXME: enabling this causes trouble with DeliteOpSuite. investigate!!
    def length(implicit pos: SourceContext) = string_length(s1)
    def +(s2: String)(implicit o: Overloaded1, pos: SourceContext) = string_plus(s1, unit(s2))
    def +(s2: Rep[String])(implicit o: Overloaded3, pos: SourceContext) = string_plus(s1, s2)
    def +(s2: Var[String])(implicit o: Overloaded4, pos: SourceContext) = string_plus(s1, readVar(s2))
    // def +(s2: Rep[Any])
    // def +(s2: Any)
    def infix_+(s1: Rep[String], s2: Rep[Any])(implicit o: Overloaded1, pos: SourceContext) = string_plus(s1, s2)
  }

  object String {
    def valueOf(a: Rep[Any])(implicit pos: SourceContext) = string_valueof(a)
  }

  def string_plus(s: Rep[Any], o: Rep[Any])(implicit pos: SourceContext): Rep[String]
  def string_startswith(s1: Rep[String], s2: Rep[String])(implicit pos: SourceContext): Rep[Boolean]
  def string_trim(s: Rep[String])(implicit pos: SourceContext): Rep[String]
  def string_split(s: Rep[String], separators: Rep[String])(implicit pos: SourceContext): Rep[Array[String]]
  def string_split(s: Rep[String], separators: Rep[String], limit: Rep[Int])(implicit pos: SourceContext): Rep[Array[String]]
  def string_valueof(d: Rep[Any])(implicit pos: SourceContext): Rep[String]
  def string_charAt(s: Rep[String], i: Rep[Int])(implicit pos: SourceContext): Rep[Char]
  def string_endsWith(s: Rep[String], e: Rep[String])(implicit pos: SourceContext): Rep[Boolean]
  def string_contains(s1: Rep[String], s2: Rep[String])(implicit pos: SourceContext): Rep[Boolean]
  def string_todouble(s: Rep[String])(implicit pos: SourceContext): Rep[Double]
  def string_tofloat(s: Rep[String])(implicit pos: SourceContext): Rep[Float]
  def string_toint(s: Rep[String])(implicit pos: SourceContext): Rep[Int]
  def string_tolong(s: Rep[String])(implicit pos: SourceContext): Rep[Long]
  def string_substring(s: Rep[String], start:Rep[Int], end:Rep[Int])(implicit pos: SourceContext): Rep[String]
  def string_length(s: Rep[String])(implicit pos: SourceContext): Rep[Int]
}

trait StringOpsExp extends StringOps with VariablesExp {
  case class StringPlus(s: Exp[Any], o: Exp[Any]) extends Def[String]
  case class StringStartsWith(s1: Exp[String], s2: Exp[String]) extends Def[Boolean]
  case class StringTrim(s: Exp[String]) extends Def[String]
  case class StringSplitS(s: Exp[String], separators: Exp[String]) extends Def[Array[String]]
  case class StringSplit(s: Exp[String], separators: Exp[String], limit: Exp[Int]) extends Def[Array[String]]
  case class StringEndsWith(s: Exp[String], e: Exp[String]) extends Def[Boolean]  
  case class StringCharAt(s: Exp[String], i: Exp[Int]) extends Def[Char]
  case class StringValueOf(a: Exp[Any]) extends Def[String]
  case class StringToDouble(s: Exp[String]) extends Def[Double]
  case class StringToFloat(s: Exp[String]) extends Def[Float]
  case class StringToInt(s: Exp[String]) extends Def[Int]
  case class StringContains(s1: Exp[String], s2: Exp[String]) extends Def[Boolean]
  case class StringToLong(s: Exp[String]) extends Def[Long]
  case class StringSubstring(s: Exp[String], start:Exp[Int], end:Exp[Int]) extends Def[String]
  case class StringLength(s: Exp[String]) extends Def[Int]

  def string_plus(s: Exp[Any], o: Exp[Any])(implicit pos: SourceContext): Rep[String] = StringPlus(s,o)
  def string_startswith(s1: Exp[String], s2: Exp[String])(implicit pos: SourceContext) = StringStartsWith(s1,s2)
  def string_trim(s: Exp[String])(implicit pos: SourceContext) : Rep[String] = StringTrim(s)
  def string_split(s: Exp[String], separators: Exp[String])(implicit pos: SourceContext) : Rep[Array[String]] = StringSplitS(s, separators)
  def string_split(s: Exp[String], separators: Exp[String], limit: Exp[Int])(implicit pos: SourceContext) : Rep[Array[String]] = StringSplit(s, separators, limit)
  def string_valueof(a: Exp[Any])(implicit pos: SourceContext) = StringValueOf(a)
  def string_charAt(s: Exp[String], i: Exp[Int])(implicit pos: SourceContext) = StringCharAt(s,i)
  def string_endsWith(s: Exp[String], e: Exp[String])(implicit pos: SourceContext) = StringEndsWith(s,e)
  def string_contains(s1: Exp[String], s2: Exp[String])(implicit pos: SourceContext) = StringContains(s1,s2)
  def string_todouble(s: Rep[String])(implicit pos: SourceContext) = StringToDouble(s)
  def string_tofloat(s: Rep[String])(implicit pos: SourceContext) = StringToFloat(s)
  def string_toint(s: Rep[String])(implicit pos: SourceContext) = StringToInt(s)
  def string_tolong(s: Rep[String])(implicit pos: SourceContext) = StringToLong(s)
  def string_substring(s: Rep[String], start:Rep[Int], end:Rep[Int])(implicit pos: SourceContext) = StringSubstring(s,start,end)
  def string_length(s: Rep[String])(implicit pos: SourceContext) = StringLength(s)

  override def mirror[A:Manifest](e: Def[A], f: Transformer)(implicit pos: SourceContext): Exp[A] = (e match {
    case StringPlus(a,b) => string_plus(f(a),f(b))
    case StringStartsWith(s1, s2) => string_startswith(f(s1), f(s2))
    case StringTrim(s) => string_trim(f(s))
    case StringSplitS(s,sep) => string_split(f(s),f(sep))
    case StringSplit(s,sep,l) => string_split(f(s),f(sep),f(l))
    case StringToDouble(s) => string_todouble(f(s))
    case StringToFloat(s) => string_tofloat(f(s))
    case StringToInt(s) => string_toint(f(s))
    case StringEndsWith(s, e) => string_endsWith(f(s),f(e))
    case StringCharAt(s,i) => string_charAt(f(s),f(i))
    case StringValueOf(a) => string_valueof(f(a))
    case StringContains(s1,s2) => string_contains(f(s1),f(s2))
    case StringSubstring(s,a,b) => string_substring(f(s),f(a),f(b))
    case StringLength(s) => string_length(f(s))
    case _ => super.mirror(e,f)
  }).asInstanceOf[Exp[A]]
}

trait ScalaGenStringOps extends ScalaGenBase {
  val IR: StringOpsExp
  import IR._
  
  override def emitNode(sym: Sym[Any], rhs: Def[Any]) = rhs match {
    case StringPlus(s1,s2) => emitValDef(sym, src"$s1+$s2")
    case StringStartsWith(s1,s2) => emitValDef(sym, src"$s1.startsWith($s2)")
    case StringTrim(s) => emitValDef(sym, src"$s.trim()")
    case StringSplitS(s, sep) => emitValDef(sym, src"$s.split($sep)")
    case StringSplit(s, sep, l) => emitValDef(sym, src"$s.split($sep,$l)")
    case StringEndsWith(s, e) => emitValDef(sym, "%s.endsWith(%s)".format(quote(s), quote(e)))    
    case StringCharAt(s,i) => emitValDef(sym, "%s.charAt(%s)".format(quote(s), quote(i)))
    case StringValueOf(a) => emitValDef(sym, src"java.lang.String.valueOf($a)")
    case StringToDouble(s) => emitValDef(sym, src"$s.toDouble")
    case StringToFloat(s) => emitValDef(sym, src"$s.toFloat")
    case StringToInt(s) => emitValDef(sym, src"$s.toInt")
    case StringToLong(s) => emitValDef(sym, src"$s.toLong")
    case StringContains(s1,s2) => emitValDef(sym, "%s.contains(%s)".format(quote(s1),quote(s2)))
    case StringSubstring(s,a,b) => emitValDef(sym, src"$s.substring($a,$b)")
    case StringLength(s) => emitValDef(sym, src"$s.length")
    case _ => super.emitNode(sym, rhs)
  }
}

trait CudaGenStringOps extends CudaGenBase {
  val IR: StringOpsExp
  import IR._
}

trait OpenCLGenStringOps extends OpenCLGenBase {
  val IR: StringOpsExp
  import IR._
}

trait CGenStringOps extends CGenBase {
  val IR: StringOpsExp
  import IR._

  override def emitNode(sym: Sym[Any], rhs: Def[Any]) = rhs match {
    case StringPlus(s1,s2) if remap(s1.tp) == "string" && remap(s2.tp) == "string" => emitValDef(sym,"string_plus(%s,%s)".format(quote(s1),quote(s2)))
    case StringStartsWith(s1,s2) => emitValDef(sym, "string_startsWith(%s,%s)".format(quote(s1),quote(s2)))
    case StringTrim(s) => emitValDef(sym, "string_trim(%s)".format(quote(s)))
    case StringSplitS(s, sep) => emitValDef(sym, "string_split(%s,%s,%s)".format(resourceInfoSym,quote(s),quote(sep)))
    case StringSplit(s, sep, lim) => emitValDef(sym, "string_split(%s,%s,%s,%s)".format(resourceInfoSym,quote(s),quote(sep),quote(lim)))
    //case StringEndsWith(s, e) => emitValDef(sym, "(strlen(%s)>=strlen(%s)) && strncmp(%s+strlen(%s)-strlen(%s),%s,strlen(%s))".format(quote(s),quote(e),quote(s),quote(e),quote(s),quote(e),quote(e)))    
    case StringCharAt(s,i) => emitValDef(sym, "string_charAt(%s,%s)".format(quote(s), quote(i)))
    //case StringValueOf(a) => 
    case StringToDouble(s) => emitValDef(sym, "string_toDouble(%s)".format(quote(s)))
    case StringToFloat(s) => emitValDef(sym, "string_toFloat(%s)".format(quote(s)))
    case StringToInt(s) => emitValDef(sym, "string_toInt(%s)".format(quote(s)))
/*
    case StringSubstring(s,a,b) => emitValDef(sym, src"({ int l=$b-$a; char* r=(char*)malloc(l); memcpy(r,((char*)$s)+$a,l); r[l]=0; r; })")
    case StringPlus(s1,s2) => s2.tp.toString match {
      // Warning: memory leaks. We need a global mechanism like reference counting, possibly release pool(*) wrapping functions.
      // (*) See https://developer.apple.com/library/mac/documentation/Cocoa/Reference/Foundation/Classes/NSAutoreleasePool_Class/Reference/Reference.html
      case "java.lang.String" => emitValDef(sym,src"({ int l1=strlen($s1),l2=strlen($s2); char* r=(char*)malloc(l1+l2+1); memcpy(r,$s1,l1); memcpy(r+l1,$s2,l2); r[l1+l2]=0; r; })")
      case "Char" => emitValDef(sym,src"({ int l1=strlen($s1); char* r=(char*)malloc(l1+2); memcpy(r,$s1,l1); r[l1]=$s2; r[l1+2]=0; r; })")
    }
    case StringToInt(s) => emitValDef(sym,src"atoi($s)")
    case StringToLong(s) => emitValDef(sym,src"atol($s)")
    case StringToFloat(s) => emitValDef(sym,src"atof($s)")
    case StringToDouble(s) => emitValDef(sym,src"atof($s)")
    case StringLength(s) => emitValDef(sym, src"strlen($s)")
    case StringTrim(s) => throw new GenerationFailedException("CGenStringOps: StringTrim not implemented yet")
    case StringSplit(s, sep) => throw new GenerationFailedException("CGenStringOps: StringSplit not implemented yet")
*/
    case _ => super.emitNode(sym, rhs)
  }
}
