import scala.collection.immutable.ListMap
import scala.scalajs.js
import scala.scalajs.js.annotation._
import js.JSConverters._

case class BracketsMatch(matched: String, index: Int)

object Operation extends Enumeration {
  type Operation = Value
  val Identity,
  Add, Sub, Mul, Div, Sqrt, Pow, Cos, Sin, Tan, Ln, Factorial,
  None = Value
}

trait BaseExpression {
  def operation: Operation.Operation
}

trait ExpressionWithChildren extends BaseExpression {
  def arguments: Array[BaseExpression]

  def copy(arguments: Array[BaseExpression] = this.arguments): ExpressionWithChildren
}

case class Expression(
                       operation: Operation.Value,
                       arguments: Array[BaseExpression],
                     ) extends BaseExpression with ExpressionWithChildren {
  override def copy(arguments: Array[BaseExpression]): ExpressionWithChildren = {
    Expression(this.operation, arguments)
  }
}

case class IdentityExpression(
                               operation: Operation.Value = Operation.Identity,
                               isReference: Boolean,
                               value: Double,
                             ) extends BaseExpression

case class BracketsExpression(
                               operation: Operation.Value = Operation.Identity,
                               arguments: Array[BaseExpression],
                             ) extends BaseExpression with ExpressionWithChildren {
  override def copy(arguments: Array[BaseExpression]): ExpressionWithChildren = {
    BracketsExpression(this.operation, arguments)
  }
}

case class OperationDefinition(
                                stringRepresentation: String,
                                argumentsOffsets: Array[Int],
                                evaluator: Array[Double] => Double,
                              )

@JSExportTopLevel("TreeBuilderAndEvaluator")
object TreeBuilderAndEvaluator extends App {
  val constants = ListMap(
    "π" -> Math.PI,
    "e" -> Math.E
  )
  val operationsDefinitions = ListMap(
    Operation.Factorial -> OperationDefinition("!", Array(-1), (arguments: Array[Double]) => (1 to arguments(0).toInt).product),
    Operation.Ln -> OperationDefinition("ln", Array(1), (arguments: Array[Double]) => Math.log(arguments(0))),
    Operation.Sin -> OperationDefinition("sin", Array(1), (arguments: Array[Double]) => Math.sin(arguments(0))),
    Operation.Cos -> OperationDefinition("cos", Array(1), (arguments: Array[Double]) => Math.cos(arguments(0))),
    Operation.Tan -> OperationDefinition("tan", Array(1), (arguments: Array[Double]) => Math.tan(arguments(0))),
    Operation.Sqrt -> OperationDefinition("√", Array(1), (arguments: Array[Double]) => Math.sqrt(arguments(0))),
    Operation.Pow -> OperationDefinition("^", Array(-1, 1),
      (arguments: Array[Double]) => Math.pow(arguments(0), arguments(1))
    ),
    Operation.Div -> OperationDefinition("÷", Array(-1, 1), (x: Array[Double]) => x.reduce(_ / _)),
    Operation.Mul -> OperationDefinition("×", Array(-1, 1), (x: Array[Double]) => x.product),
    Operation.Sub -> OperationDefinition("-", Array(-1, 1), (x: Array[Double]) => x.reduce(_ - _)),
    Operation.Add -> OperationDefinition("+", Array(-1, 1), (x: Array[Double]) => x.sum),
  )

  def getInnerContent(str: String, deep: Int = 0): String = {
    if (str.length == 0) return str

    val firstChar = str.charAt(0)
    if (firstChar == ')' && deep == 1) return ")"

    val restOfString = str.substring(1)
    val updatedDeep = if (firstChar == '(') deep + 1 else if (firstChar == ')') deep - 1 else deep

    firstChar + getInnerContent(restOfString, updatedDeep)
  }

  def getMostShallowBrackets(str: String, index: Int = 0): BracketsMatch = {
    if (str.length == 0) return null
    val firstChar = str.charAt(0)
    if (firstChar == '(')
      return BracketsMatch(matched = getInnerContent(str), index = index)
    val restOfString = str.substring(1)
    val recursionStep = getMostShallowBrackets(restOfString, index + 1)
    recursionStep
  }

  /*
    Input:
      3+(4^2+5^(1+1)÷(4+2))×6
    Output:
      where @n = stack[n] and stack is equal to:
      [
        "3+@1×6",
        "4^2+5^@2÷@3",
        "1+1",
        "4+2",
      ]
    */
  def unwrapBrackets(stack: Array[String], index: Int = 0): Array[String] = {
    val expressionAsString = stack(index)
    val mostShallowBrackets = getMostShallowBrackets(expressionAsString)
    if (mostShallowBrackets != null) {
      val wholeMatch = mostShallowBrackets.matched
      val matchBody = wholeMatch.substring(1).dropRight(1)
      val expressionId = stack.length

      val beforeBracketsRange = (
        0,
        mostShallowBrackets.index
      )
      val afterBracketsRange = (
        mostShallowBrackets.index + wholeMatch.length,
        expressionAsString.length
      )

      val updatedExpressionAsString =
        expressionAsString.substring(beforeBracketsRange._1, beforeBracketsRange._2) +
          s"@$expressionId" +
          expressionAsString.substring(afterBracketsRange._1, afterBracketsRange._2)

      val updatedQueue =
        (stack.slice(0, index) :+
          updatedExpressionAsString) ++
          (stack.drop(index + 1) :+
            matchBody)

      val selfUpdate = unwrapBrackets(updatedQueue, index)
      unwrapBrackets(
        selfUpdate,
        updatedQueue.length - 1 // we should unwrap matchBody
      )
    } else
      stack
  }

  def combineAtSign(splitedString: Array[String]): Array[String] = {
    val isFloat = "[0-9\\.]+"
    val isLetter = "[a-z]+"
    if (splitedString.length <= 1)
      splitedString
    else {
      val currentChar = splitedString.last
      val nextChar = splitedString(splitedString.length - 2)
      if (nextChar == "@") {
        combineAtSign(splitedString.dropRight(2)) :+ (nextChar ++ currentChar)
      } else if (constants.get(currentChar).isDefined && nextChar.matches(isFloat)) {
        combineAtSign(splitedString.dropRight(1) :+ operationsDefinitions(Operation.Mul).stringRepresentation :+ currentChar)
      } else if (currentChar.matches(isFloat)) {
        val digitsCountFromEnd = splitedString.reverse.takeWhile(_.matches(isFloat)).reverse // = takeRightWhile(isDigit)
        val reducedValue = digitsCountFromEnd.reduce(_ ++ _)
        combineAtSign(splitedString.dropRight(digitsCountFromEnd.length)) :+ reducedValue
      } else if (currentChar.matches(isLetter)) {
        val lettersFromEnd = splitedString.reverse.takeWhile(_.matches(isLetter)).reverse // = takeRightWhile(isDigit)
        val reducedValue = lettersFromEnd.reduce(_ ++ _)
        combineAtSign(splitedString.dropRight(lettersFromEnd.length)) :+ reducedValue
      } else
        combineAtSign(splitedString.dropRight(1)) :+ currentChar
    }
  }


  def parseString(expressionAsString: String): Array[BaseExpression] = {
    val valueRegex = "@[0-9]+|[0-9\\.]+"
    val constantsJoined = constants.keys.reduce(_ ++ _)
    val splitRegex = s"$valueRegex|[a-z]+|[$constantsJoined]+"
    val zeroWidthRegex = s"(?<=$splitRegex)|(?=$splitRegex)"
    val tokenizedString =
      combineAtSign(
        expressionAsString
          .split(zeroWidthRegex) // split by values but does'nt delete them
          .filter(_ != null)
      )
    val parsedString: Array[BaseExpression] =
      tokenizedString
        .zipWithIndex
        .map { case (s: String, index: Int) =>
          if (s.matches(valueRegex)) {
            if (s.charAt(0) == '@')
              IdentityExpression(isReference = true, value = s.substring(1).toDouble)
            else
              IdentityExpression(isReference = false, value = s.toDouble)
          } else if (constants.get(s).isDefined) {
            IdentityExpression(isReference = false, value = constants(s))
          } else {
            val operationDefinition =
              operationsDefinitions
                .find(_._2.stringRepresentation == s)
            if (operationDefinition.isDefined) {
              val operation = operationDefinition.get._1
              Expression(operation, Array())
            } else
              null
          }
        }
    parsedString
  }

  /*
  for input: 1 + 2 × 3 × 4 - 5 × 6 and the definition of multiply operation,
  we should transform this array into: 1 + #1 - #2
  where:
  #1 = expression of operation multiply with arguments 2, 3, 4
  #2 = expression of operation multiply with arguments 5, 6
   */
  def buildTreeForOperationType(arrayOfExpressions: Array[BaseExpression], operation: (Operation.Value, OperationDefinition), fromIndex: Int = 0): Array[BaseExpression] = {
    val firstAppearanceOfOperation = arrayOfExpressions
      .zipWithIndex
      .find { case (expression, i) =>
        expression.operation == operation._1 && i >= fromIndex
      }
    if (firstAppearanceOfOperation.isDefined) {
      val firstAppearanceOfOperationIndex = firstAppearanceOfOperation.get._2
      val arguments = operation._2.argumentsOffsets.map { argOffset => arrayOfExpressions(firstAppearanceOfOperationIndex + argOffset) }
      val node = Expression(operation._1, arguments)
      val affectedIndices = (operation._2.argumentsOffsets :+ 0).map { argOffset => firstAppearanceOfOperationIndex + argOffset }
      buildTreeForOperationType(
        arrayOfExpressions.slice(0, affectedIndices.min) ++
          Array(node) ++
          arrayOfExpressions.drop(affectedIndices.max + 1),
        operation,
        firstAppearanceOfOperationIndex + 1
      )
    } else
      arrayOfExpressions
  }

  def buildTree(arrayOfExpressions: Array[BaseExpression], definitions: Map[Operation.Value, OperationDefinition]): Array[BaseExpression] = {
    if (definitions.isEmpty)
      return arrayOfExpressions
    val current = definitions.head
    val restOfDefinitions = definitions.drop(1)

    buildTree(
      buildTreeForOperationType(
        arrayOfExpressions,
        current
      ),
      restOfDefinitions
    )
  }

  def referencesRestore(node: BaseExpression, stackTrees: Array[BaseExpression]): BaseExpression = {
    val selfUpdated = node match {
      case identity: IdentityExpression =>
        if (identity.isReference) {
          BracketsExpression(
            arguments = Array(stackTrees(identity.value.toInt))
          )
        } else node
      case expression => expression
    }
    val argumentsUpdated = selfUpdated match {
      case expression: ExpressionWithChildren =>
        val updatedArguments = expression
          .arguments
          .map(arg => referencesRestore(arg, stackTrees))
        expression.copy(arguments = updatedArguments)
      case identity => identity // node have no children
    }
    argumentsUpdated
  }

  @JSExport
  def buildTreeFromString(query: String): BaseExpression = {
    val stack = unwrapBrackets(Array(query))

    val parsedStack = stack map parseString
    val stackTrees = parsedStack.map(parseString => buildTree(parseString, operationsDefinitions)).map(tree => tree.head)
    val resultTree = stackTrees.head
    val replaceReferences = referencesRestore(resultTree, stackTrees)
    replaceReferences
  }

  @JSExport
  def evaluateTree(root: BaseExpression): js.Dictionary[Object] = {
    root match {
      case identity: IdentityExpression =>
        val value = identity.value
        val maybeConstant = constants find { case (_, actualValue) =>
          value == actualValue
        } map { case (representation, _) =>
          representation
        }
        val valueAsString = value.toString
        val valueToDisplay = maybeConstant getOrElse valueAsString
        val resultMap = Map(
          "value" ->
            valueToDisplay,
          "children" ->
            Array().toJSArray,
          "secondaryValue" ->
            valueAsString,
        ).toJSDictionary
        resultMap
      case brackets: BracketsExpression =>
        val argsEvaluated = brackets.arguments map { arg => evaluateTree(arg) }
        val secondaryValue = argsEvaluated.head.get("secondaryValue").get.toString
        js.Dictionary(
          "value" -> "()",
          "children" -> argsEvaluated.toJSArray,
          "secondaryValue" -> secondaryValue
        )
      case expression: Expression =>
        val argsEvaluated = expression.arguments map { arg => evaluateTree(arg) }
        val operation = expression.operation
        val valueToDisplay = operationsDefinitions(operation).stringRepresentation
        val argsValue = argsEvaluated map { arg =>
          arg.get("secondaryValue").get.toString.toDouble
        }
        val secondaryValue = operationsDefinitions(expression.operation).evaluator(argsValue)
        js.Dictionary(
          "value" -> valueToDisplay,
          "children" -> argsEvaluated.toJSArray,
          "secondaryValue" -> secondaryValue.toString
        )
    }
  }

  /*
    Example:
    3+(4^2+2531^cos(1÷3π)÷(4+2))×6 = 149.30904491242111
    note: 1÷3π = 1÷3×π = (1÷3)×π
  */
  val t = buildTreeFromString("101.2360679774998×2")
}

/*
Algorithm:
1.  Replace brackets with symbols (@1)
2.  Split every expression and symbol into array of numbers/symbols and operations,
    Represent numbers or symbols using the identity operation.
3.  Build a tree as the identity operations are leaves,
    and other operations are regular nodes.
    Make sure that the tree satisfying operator precedence.
4.  Evaluating the expression according to tree using recursion.
*/
