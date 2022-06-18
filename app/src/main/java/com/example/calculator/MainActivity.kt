package com.example.calculator

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.view.isVisible
import androidx.room.Room
import com.example.calculator.model.History
import java.lang.NumberFormatException

class MainActivity : AppCompatActivity() {


    private val expressionTextView: TextView by lazy {
        findViewById<TextView>(R.id.expressionTextView)
    }

    private val resultTextView: TextView by lazy {
        findViewById<TextView>(R.id.resultTextView)
    }

    private val historyLayout: View by lazy {
        findViewById<View>(R.id.historyLayout)
    }

    private val historyLinearLayout: LinearLayout by lazy {
        findViewById<LinearLayout>(R.id.historyLinearLayout)
    }

    lateinit var db: AppDatabase

    private var isOperator = false
    private var hasOperator = false




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "historyDB"
        ).build()
    }


    @RequiresApi(Build.VERSION_CODES.M)
    fun buttonClicked(v: View) {
        when (v.id) {
            R.id.button0 -> numberClicked("0")
            R.id.button1 -> numberClicked("1")
            R.id.button2 -> numberClicked("2")
            R.id.button3 -> numberClicked("3")
            R.id.button4 -> numberClicked("4")
            R.id.button5 -> numberClicked("5")
            R.id.button6 -> numberClicked("6")
            R.id.button7 -> numberClicked("7")
            R.id.button8 -> numberClicked("8")
            R.id.button9 -> numberClicked("9")
            R.id.buttonPlus -> operatorClicked("+")
            R.id.buttonMinus -> operatorClicked("-")
            R.id.buttonMulti -> operatorClicked("*")
            R.id.buttonModulo -> operatorClicked("/")
            R.id.buttonDivider -> operatorClicked("%")
        }

    }

    private fun numberClicked(number: String) {

        if (isOperator) {
            expressionTextView.append(" ")
        }

        isOperator = false


        val expressionText = expressionTextView.text.split(" ")
        if (expressionText.isNotEmpty() && expressionText.last().length >= 15) {
            Toast.makeText(this, "15자리까지만 사용할 수 있습니다.", Toast.LENGTH_SHORT).show()
            return
        } else if (expressionText.last().isEmpty() && number == "0") {
            Toast.makeText(this, "0은 맨 앞자리에 사용할 수 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        expressionTextView.append(number)
        resultTextView.text = calculateExpression()

    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun operatorClicked(operator: String) {

        if (expressionTextView.text.isEmpty()) {
            return
        }

        when {
            isOperator -> {
                val text = expressionTextView.text.toString()
                expressionTextView.text = text.dropLast(1) + operator
            }

            hasOperator -> {
                Toast.makeText(this, "연산자는 한 번만 사용할 수 있습니다.", Toast.LENGTH_SHORT).show()
                return
            }
            else -> {
                expressionTextView.append(" $operator")

            }
        }

        val ssb = SpannableStringBuilder(expressionTextView.text)
        ssb.setSpan(
            ForegroundColorSpan(getColor(R.color.green)),
            expressionTextView.text.length - 1,
            expressionTextView.text.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        expressionTextView.text = ssb
        isOperator = true
        hasOperator = true

    }

    fun resultbuttonClicked(v: View) {
        val expressionTexts = expressionTextView.text.split(" ")

        if (expressionTextView.text.isEmpty() || expressionTexts.size == 1) {
            return
        }

        if (expressionTexts.size != 3 && hasOperator) {
            Toast.makeText(this, "미완성 수식입니다.", Toast.LENGTH_SHORT).show()
            return
        }
        if (expressionTexts[0].isNumber().not() || expressionTexts[2].isNumber().not()) {
            Toast.makeText(this, "미완성 수식입니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val expressionText = expressionTextView.text.toString()
        val resultText = calculateExpression()

        Thread(Runnable {
            db.historyDao().insertHistory(History(null,expressionText,resultText))
        }).start()



        resultTextView.text = ""
        expressionTextView.text = resultText
        isOperator = false
        hasOperator = false

    }

    private fun calculateExpression(): String {
        val expressionTexts = expressionTextView.text.split(" ")

        if (hasOperator.not() || expressionTexts.size != 3) {
            return ""
        } else if (expressionTexts[0].isNumber().not() || expressionTexts[2].isNumber().not()) {
            return ""
        }

        val exp1 = expressionTexts[0].toBigInteger()
        val exp2 = expressionTexts[2].toBigInteger()
        val op = expressionTexts[1]

        return when (op) {
            "+" -> (exp1 + exp2).toString()
            "-" -> (exp1 - exp2).toString()
            "*" -> (exp1 * exp2).toString()
            "/" -> (exp1 / exp2).toString()
            "%" -> (exp1 % exp2).toString()
            else -> ""

        }
    }


    fun clearbuttonClicked(v: View) {

        expressionTextView.text = ""
        resultTextView.text = ""
        isOperator = false
        hasOperator = false
    }

    fun historybuttonClicked(v: View) {
          historyLayout.isVisible = true


          historyLinearLayout.removeAllViews()

          Thread(Runnable {
              db.historyDao().getAll().reversed().forEach {

                  runOnUiThread {
                      val historyView = LayoutInflater.from(this).inflate(R.layout.history_row, null , false)
                      historyView.findViewById<TextView>(R.id.expressionTextView).text = it.expression
                      historyView.findViewById<TextView>(R.id.resultTextView).text = "= ${it.result}"

                      historyLinearLayout.addView(historyView)
                  }

              }
          }).start()

        //TODO db에서 모든 기록 가져오기
        //TODO 뷰에서 모든 기록 가져오기
    }

    fun closeHistoryButtonClicked(v: View) {

        historyLayout.isVisible = false
    }


    fun historyClearButtonClicked(v: View) {

        historyLinearLayout.removeAllViews()

        Thread(Runnable {
            db.historyDao().deleteAll()
        }).start()


        //TODO db에서 모든 기록 삭제

        //TODO 뷰에서 모든 기록 삭제
    }


    fun String.isNumber(): Boolean {
        return try {
            this.toBigInteger()
            return true
        } catch (e: NumberFormatException) {
            false
        }

    }
}