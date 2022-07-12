package com.example.rxapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.annotation.WorkerThread
import com.example.rxapp.databinding.ActivityMainBinding
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import java.lang.StringBuilder
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var compositeDisposable: CompositeDisposable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        compositeDisposable = CompositeDisposable()

        binding.submitRxButton.setOnClickListener {
//            val inputText = binding.inputEditText.text.toString()
//            Observable.just(binding.inputEditText.text)  // <- запуск, но все работает не в главном потоке
//            Observable.fromIterable(binding.inputEditText.text.toString().toCharArray().toList())  // <- запуск, но все работает не в главном потоке
            compositeDisposable.add(Observable.just(binding.inputEditText.text)  // <- запуск, но все работает не в главном потоке
                .map { it.toString() }
                .filter { it.isNotBlank() }
                .map { it.uppercase() }
                .map { it.reversed() }
                .observeOn(Schedulers.computation())  // <- переключаемся на другой поток
                .map { changeString(it) }
//                .doOnNext { throw RuntimeException("Чтото произошло")}
                .observeOn(Schedulers.io())
                .map { appendDate(it) }
                .observeOn(AndroidSchedulers.mainThread())  // <- возвращаемся в главный поток
                .subscribeBy (
                    onNext =  {
                        binding.resultTextView.text = it
                    } ,
                    onError =  {
                        binding.resultTextView.text = it.message
                    },
                    onComplete = {
                        binding.resultTextView.text = "Закончила"
                    }
                )) // <- подписываемся, по сути это запуск поточности

        }

        binding.submitButton.setOnClickListener {
            val inputText = binding.inputEditText.text.toString()
            val upperText = inputText.uppercase()
            val reversedText = upperText.reversed()
            Thread {
                try {
                    val changedText = changeString(reversedText)
                    Thread {
                        try {
                            val dateText = appendDate(changedText)
                            runOnUiThread{
                                binding.resultTextView.text = dateText
                            }

                        } catch (ie: InterruptedException) {
                            // todo
                        }
                    }.start()
                } catch (ie: InterruptedException) {
                    // todo
                }
            }.start()
        }

        binding.buttonGo.setOnClickListener {
            val observable1 = Observable.just(binding.num1EditText.text)
                .map { it.toString() }
                .map { it.toInt() }
                .observeOn(Schedulers.computation())
                .doOnNext{Thread.sleep(2_000)}
                .map { it * 2 }

            val observable2 = Observable.just(binding.num2EditText.text)
                .map { it.toString() }
                .map { it.toInt() }
                .observeOn(Schedulers.computation())
                .doOnNext{Thread.sleep(3_000)}
                .map { it * it }

            val disposable =  Observable
                .zip(observable1, observable2) { num1, num2 ->
                num1 + num2
            }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    val oldText = binding.goTextView.text.toString()
                    binding.goTextView.text = "$oldText $it"
                }
            compositeDisposable.add(disposable)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.dispose()
    }

    @WorkerThread
    private fun changeString(input: String): String {
        val sb = StringBuilder()
        Thread.sleep(3_000)
        input.forEach {
            sb.append(it)
            sb.append(" ")
        }
        return  sb.toString()
    }
    @WorkerThread
    private fun appendDate(input: String): String {
        Thread.sleep(1_000)
        val date = Calendar.getInstance().time
        val dateString = SimpleDateFormat().format(date)

        return "$input $dateString"
    }

}