import java.io.StringReader
import org.xml.sax.InputSource
import org.specs2.mutable._
import com.chrisrebert.lmvtfy.validation._

class ValidatorSpec extends Specification {
  implicit class HtmlString(str: String) {
    def inputSource: InputSource = new InputSource(new StringReader(str))
  }

  val httpEquivErrText = Vector(PlainText("Bad value "), CodeText("Gibberish"), PlainText(" for attribute "), CodeText("http-equiv"), PlainText(" on XHTML element "), CodeText("meta"), PlainText("."))
  val httpEquivErrSpan = SourceSpan(6, 57, 6, 57).get
  val httpEquivValidationMsg = ValidationMessage(Some(httpEquivErrSpan), httpEquivErrText)

  "Bad meta http-equiv" should {
    val badHtml =
      """
        | <!DOCTYPE html>
        | <html lang="en">
        |   <head>
        |     <meta charset="utf-8">
        |     <meta http-equiv="Gibberish" content="gobbledegook">
        |     <title>Title</title>
        |   </head>
        |   <body></body>
        | </html>
      """.stripMargin

    "cause a validation error" in {
      val messages = Html5Validator.validationErrorsFor(badHtml.inputSource)
      messages must have size(1)
      messages.head mustEqual httpEquivValidationMsg
    }
  }

  "Bad meta http-equiv and non-table-related child of a table" should {
    val badHtml =
      """
        | <!DOCTYPE html>
        | <html lang="en">
        |   <head>
        |     <meta charset="utf-8">
        |     <meta http-equiv="Gibberish" content="gobbledegook">
        |     <title>Title</title>
        |   </head>
        |   <body>
        |     <ul>
        |       <p>Can't just randomly put a paragraph tag here!</p>
        |     </ul>
        |   </body>
        | </html>
      """.stripMargin

    "cause 2 validation errors" in {
      val pInUlMsg = ValidationMessage(SourceSpan(11, 10, -1, -1), Vector(PlainText("HTML element "), CodeText("p"), PlainText(" not allowed as child of HTML element "), CodeText("ul"), PlainText(" in this context. (Suppressing further errors from this subtree.)")))

      val messages = Html5Validator.validationErrorsFor(badHtml.inputSource)
      messages must have size(2)
      messages(0) mustEqual httpEquivValidationMsg
      messages(1) mustEqual pInUlMsg
    }
  }
}
