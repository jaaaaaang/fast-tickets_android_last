package com.wm.fasttickets
//package com.example.fasttickets

import androidx.appcompat.app.AppCompatDelegate

import android.app.DatePickerDialog
import android.content.pm.PackageManager
import android.Manifest
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import android.widget.EditText
import android.widget.ImageView

import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import java.util.*
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.Response
import retrofit2.Retrofit

import android.content.Intent

import android.widget.Button
//리워드 광고

import com.google.android.gms.ads.*
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback


import androidx.viewpager2.widget.ViewPager2
import com.example.fasttickets.SlideAdapter
import android.view.ViewGroup


import android.content.Context
import java.io.InputStream
import java.security.KeyStore
import java.security.cert.CertificateFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import okhttp3.OkHttpClient
import javax.net.ssl.X509TrustManager
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.WindowCompat


fun getCustomOkHttpClient(context: Context): OkHttpClient {
    // 인증서를 로드
    val cf = CertificateFactory.getInstance("X.509")
    val caInput: InputStream = context.resources.openRawResource(R.raw.isrgrootx1) // 인증서 파일
    val ca = caInput.use {
        cf.generateCertificate(it)
    }

    // KeyStore 생성 및 인증서 추가
    val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
    keyStore.load(null, null)
    keyStore.setCertificateEntry("ca", ca)

    // TrustManagerFactory 초기화
    val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
    tmf.init(keyStore)

    // SSLContext 초기화
    val sslContext = SSLContext.getInstance("TLS")
    sslContext.init(null, tmf.trustManagers, null)

    // OkHttpClient 빌드
    return OkHttpClient.Builder()
        .sslSocketFactory(sslContext.socketFactory, tmf.trustManagers[0] as X509TrustManager)
        .build()
}


class MainActivity : AppCompatActivity() {

    // 리워드광고를 위한 변수선언
//    0320
    private var rewardedAd: RewardedAd? = null
//    private var rewardedAd: RewardedInterstitialAd? = null
    // 전면 광고를 위한 변수
//    private var interstitialAd: InterstitialAd? = null

    // 보상형 전면 광고를 위한 변수
    private var rewardedInterstitialAd: RewardedInterstitialAd? = null

    //    fcm
    private var fcmToken: String? = null // FCM 토큰 저장 변수

    private lateinit var transportSwitch: Switch
    private lateinit var retrofit: Retrofit
    private lateinit var apiService: ApiService
    private lateinit var adView: AdView
    private lateinit var editPhoneNumber: EditText
    private lateinit var editPassword: EditText
    private lateinit var spinnerDeparture: Spinner
    private lateinit var spinnerArrival: Spinner
    private lateinit var selectedDate: TextView

    private lateinit var btnSubmit: Button
    private lateinit var btnSelectDate: Button
    private var selectedDateValue: String? = null

    private lateinit var spinnerHour: Spinner
    private lateinit var spinnerMinute: Spinner
    private var selectedHour: String = "05" // 기본값
    private var selectedMinute: String = "00" // 기본값

    // privilege_type 우대정보
    private lateinit var privilege_spinner: Spinner


    private lateinit var textKTX: TextView
    private lateinit var textSRT: TextView
//    private var textKTX: TextView? = null
//    private var textSRT: TextView? = null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // 알림 권한 요청
        requestNotificationPermission()

        // Firebase 초기화
        FirebaseApp.initializeApp(this)
        // FCM 토큰 생성 및 저장
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM", "FCM 토큰 생성 실패", task.exception)
                return@addOnCompleteListener
            }
            // 토큰 저장
            fcmToken = task.result
            Log.d("FCM", "FCM 토큰: $fcmToken")
        }


        // AdMob SDK 초기화
        MobileAds.initialize(this) { }

        // 배너 광고 초기화 및 로드
        adView = findViewById(R.id.adView)
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)

        // 보상형 전면 광고 로드
        loadRewardedInterstitialAd()
        // 리워드 광고 로드
        loadRewardedAd()

        // Retrofit 초기화
        retrofit = Retrofit.Builder()
            .baseUrl("https://fasttickets.duckdns.org:5002/")  // 기본 URL (SRT)
            .client(getCustomOkHttpClient(this))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiService = retrofit.create(ApiService::class.java)

        // UI 요소 초기화
        transportSwitch = findViewById(R.id.switchTransport)
        textKTX = findViewById(R.id.textKTX) // textKTX 초기화
        textSRT = findViewById(R.id.textSRT) // textSRT 초기화

        editPhoneNumber = findViewById(R.id.editPhoneNumber)
        editPassword = findViewById(R.id.editPassword)
        spinnerDeparture = findViewById(R.id.spinnerDeparture)
        spinnerArrival = findViewById(R.id.spinnerArrival)
        selectedDate = findViewById(R.id.selectedDate)
        btnSubmit = findViewById(R.id.btnSubmit)
        btnSelectDate = findViewById(R.id.btnSelectDate)

        privilege_spinner = findViewById(R.id.privilege_spinner)


        // 광고보기 위한 변수
        val watchadbt = findViewById<Button>(R.id.watchadbt)

        // 광고보기 버튼 클릭 이벤트
        watchadbt.setOnClickListener {
//            showInterstitialAd() // 전면 광고 표시
            // 유효성 검사
            val phoneNumber = editPhoneNumber.text.toString().trim()
            if (phoneNumber.isEmpty() ) {
                showAlertDialog("로그인", "휴대폰 정보를 입력하세요")
            }
            else{showRewardedAd() //post 포함 하고있음
                loadRewardedAd()
            }
        }

        // 공유하기 를 위한 변수
        val shareButton = findViewById<Button>(R.id.shareButton)
        // 공유하기 버튼 클릭 이벤트
        shareButton.setOnClickListener {
            val phoneNumber = editPhoneNumber.text.toString().trim()
            if (phoneNumber.isEmpty() ) {
                showAlertDialog("로그인", "휴대폰 정보를 입력하세요")
            }
            else{
                shareAppLink()
                sendRewards_ads(phoneNumber)

            }

        }


//        햄버거 버튼 초기화
        val menuButton = findViewById<ImageView>(R.id.my) // 햄버거 버튼

        menuButton.setOnClickListener {
            val phoneNumber = editPhoneNumber.text.toString().trim()
            // POST API 호출
            sendReservationRequest_menu(phoneNumber)
        }






//   시간 UI
        spinnerHour = findViewById(R.id.spinnerHour)
        spinnerMinute = findViewById(R.id.spinnerMinute)

        // 시간 스피너 설정
        ArrayAdapter.createFromResource(
            this,
            R.array.hour_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerHour.adapter = adapter
        }

        // 분 스피너 설정
        ArrayAdapter.createFromResource(
            this,
            R.array.minute_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerMinute.adapter = adapter
        }

        // 시간 선택 리스너
        spinnerHour.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View,
                position: Int,
                id: Long
            ) {
                selectedHour = parent.getItemAtPosition(position).toString()
//                Toast.makeText(this@MainActivity, "선택된 시간: $selectedHour 시", Toast.LENGTH_SHORT).show()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // 분 선택 리스너
        spinnerMinute.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View,
                position: Int,
                id: Long
            ) {
                selectedMinute = parent.getItemAtPosition(position).toString()
//                Toast.makeText(this@MainActivity, "선택된 분: $selectedMinute 분", Toast.LENGTH_SHORT).show()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }


        btnSubmit.setOnClickListener {
            sendReservationRequest()
//            showInterstitialAd()
        }


        // 날짜 선택 클릭 리스너
        btnSelectDate.setOnClickListener {
            showDatePickerDialog()
        }



// Switch 상태에 따라 BASE_URL 변경
        transportSwitch.isChecked = false // 초기값 설정
        val isChecked = transportSwitch.isChecked

// 초기값에 따라 else 구문 동작
        if (isChecked) {
            //여기 프리빌리지 추가 KTX
            setup_KTX_P_Spinners()
            setupDepartureArrivalSpinners_ktx()  // 출발/도착역 스피너 설정
            textKTX.apply {
                textSize = 20f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
            textSRT.apply {
                textSize = 16f
                setTypeface(null, android.graphics.Typeface.NORMAL)
            }
//            val baseUrl = "http://39.116.5.105:5001/" // KTX
        } else {
            //여기 프리빌리지 추가 SRT
            setup_SRT_P_Spinners()
            setupDepartureArrivalSpinners()  // 출발/도착역 스피너 설정
            textSRT.apply {
                textSize = 20f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
            textKTX.apply {
                textSize = 16f
                setTypeface(null, android.graphics.Typeface.NORMAL)
            }
//            val baseUrl = "http://39.116.5.105:5002/" // SRT
        }


// Switch 상태에 따라 BASE_URL 변경
        transportSwitch.setOnCheckedChangeListener { _, isChecked ->
            val baseUrl = if (isChecked) {
                //여기 프리빌리지 추가 KTX
                setup_KTX_P_Spinners()
                setupDepartureArrivalSpinners_ktx()  // 출발/도착역 스피너 설정
                textKTX.apply {
                    textSize = 20f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                }
                textSRT.apply {
                    textSize = 16f
                    setTypeface(null, android.graphics.Typeface.NORMAL)
                }
                "https://fasttickets.duckdns.org:5001" // KTX
            } else {
                //여기 프리빌리지 추가 SRT
                setup_SRT_P_Spinners()
                setupDepartureArrivalSpinners()  // 출발/도착역 스피너 설정
                textSRT.apply {
                    textSize = 20f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                }
                textKTX.apply {
                    textSize = 16f
                    setTypeface(null, android.graphics.Typeface.NORMAL)
                }
                "https://fasttickets.duckdns.org:5002" // SRT
            }

            retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }

        fetchNotices()  // 초기 로딩 시 공지사항 가져오기

    }


//    onCreate 끝


    //공유하기 버튼 기능
    private fun shareAppLink() {
        // 앱 다운로드 링크 (Play Store에 앱을 배포한 경우 링크 사용)
        val appLink = "https://www.notion.so/FastTickets-App-1cc93f413bf680d6b42bf1a67d5aa182"  //"https://play.google.com/store/apps/details?id=com.example.myapp"

        // 공유 인텐트 생성
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "이 앱을 소문내 주세요~!! ")
            putExtra(Intent.EXTRA_TEXT, "이 앱 정말 좋아요! 아래 링크에서 다운받아보세요:\n$appLink")
        }

        // 공유 Intent 실행
        try {
            startActivity(Intent.createChooser(shareIntent, "앱 공유하기"))
        } catch (e: Exception) {
//            Toast.makeText(this, "공유할 수 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }






    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13 이상
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    100
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 알림 권한이 허용됨
            } else {
                // 알림 권한이 거부됨
            }
        }
    }



//우대정보
    private fun setup_SRT_P_Spinners() {
        val privilegeList = listOf("어른","어린이","경로","중증장애인","경증장애인")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, privilegeList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        privilege_spinner.adapter = adapter
    }
    private fun setup_KTX_P_Spinners() {
        val privilegeList = listOf("어른","어린이","유아","경로","중증장애인","경증장애인","국가유공자")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, privilegeList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        privilege_spinner.adapter = adapter
    }






    private fun setupDepartureArrivalSpinners() {
        val stationList = listOf("수서", "동탄", "평택지제","천안아산","오송","대전","동대구","울산(통도사)","부산","익산","광주송정","목포","김천구미","경주","공주","곡성","구례구","나주","남원","밀양","마산","서대구","순천","익산","여천","여수EXPO","정읍","전주","진영","진주","창원중앙","창원", "포항",)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, stationList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

//        자동완성기능 테스트 (한글안됨 처리필요)
//        val autoCompleteTextView: AutoCompleteTextView = findViewById(R.id.autoCompleteTextView)
//        val adapter3 = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, stationList)
//        autoCompleteTextView.setAdapter(adapter3)

        spinnerDeparture.adapter = adapter
        spinnerArrival.adapter = adapter
    }

    private fun setupDepartureArrivalSpinners_ktx() {
        val stationList = listOf("서울","광명","천안아산","오송","대전","김천구미","서대구","동대구","경주","밀양","울산","구포","부산","마산","진주","영등포","수원","경산","물금","창원중앙","창원","포항","정읍","진영","김제","남원","곡성","구례구","순천","여천","서대전","공주","전주","계룡","논산","용산","익산","장성","광주송정","나주","목포","여수EXPO","행신",)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, stationList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDeparture.adapter = adapter
        spinnerArrival.adapter = adapter
    }

    // 날짜 선택 다이얼로그
    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(this, { _, y, m, d ->
            // 날짜 포맷을 "yyyy/MM/dd" 형식으로 두 자리 숫자로 설정
            val formattedDate = String.format("%04d/%02d/%02d", y, m + 1, d) // 두 자리로 맞추기
            selectedDateValue = formattedDate
            selectedDate.text = "선택된 날짜: $selectedDateValue"
        }, year, month, day).show()
    }

//    // 전면 광고 로드 함수
//    private fun loadInterstitialAd() {
//        val adRequest = AdRequest.Builder().build()
//        InterstitialAd.load(
//            this,
//            "ca-app-pub-3940256099942544/1033173712", // 테스트 전면 광고 ID
//            adRequest,
//            object : InterstitialAdLoadCallback() {
//                override fun onAdLoaded(ad: InterstitialAd) {
//                    interstitialAd = ad
//                    println("Interstitial Ad Loaded Successfully.")
//                }
//
//                override fun onAdFailedToLoad(error: LoadAdError) {
//                    interstitialAd = null
//                    println("Interstitial Ad Failed to Load: ${error.message}")
//                }
//            }
//        )
//    }
//
//    // 전면 광고 표시 함수
//    private fun showInterstitialAd() {
//        interstitialAd?.show(this) ?: println("Interstitial Ad is not ready yet.")
//    }



    private fun showAlertDialog(title: String, message: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("확인") { dialog, _ ->
                dialog.dismiss() // 팝업 닫기
            }
            .setCancelable(false) // 바깥 영역을 눌러도 닫히지 않게 설정
            .show()
    }

//    private fun showAlertDialog_menu(title: String, message: String) {
//        val builder = AlertDialog.Builder(this)
//        val dialog = builder.setTitle(title)
//            .setMessage(message)
//            .setPositiveButton("확인") { dialog, _ ->
//                dialog.dismiss() // 팝업 닫기
//            }
//            .setCancelable(false) // 바깥 영역을 눌러도 닫히지 않게 설정
//            .create()
//
//        // 팝업 띄우기 전에 Window 설정
//        dialog.show() // 반드시 show()를 먼저 호출해야 window 객체 접근 가능
//        dialog.window?.apply {
//            // 팝업 크기 조정
//            setLayout(
//                (resources.displayMetrics.widthPixels * 0.8).toInt(), // 가로 크기 (화면의 80%)
//                (resources.displayMetrics.heightPixels * 0.8).toInt() // 세로 크기 (화면의 50%)
//            )
//
//            // 팝업 외부 어둡게
//            setDimAmount(0.9f)
//        }
//    }
    // 슬라이더 점점점 표시 추가? + 라운드박스 + 이쁜이 작업(추가설명)



    //리워드 광고 함수
//    ca-app-pub-3989916427250119/9331631930
    private fun loadRewardedAd() {
        // 광고 요청 생성
        val adRequest = AdRequest.Builder().build()
//        ca-app-pub-3989916427250119/9331631930
//        ca-app-pub-3989916427250119/5516176861
        // 리워드 광고 로드 (테스트 광고 ID 사용)  RewardedInterstitialAd
        RewardedAd.load(this, "ca-app-pub-3989916427250119/5516176861", adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
//                    Toast.makeText(applicationContext, "리워드 광고 로드 완료!", Toast.LENGTH_SHORT).show()
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    rewardedAd = null
                    val errorMessage = "리워드 광고 로드 실패: ${loadAdError.code} - ${loadAdError.message}"
                    Log.e("AdMob", errorMessage)
//                    Toast.makeText(applicationContext, errorMessage, Toast.LENGTH_LONG).show()
//                    Toast.makeText(applicationContext, "11광고 로드 실패: ${loadAdError.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    //지금할꺼 : 보상 받으면 post 보내기.  (보안, 디자인, 포스트)
    private fun showRewardedAd() {
        // 광고가 로드된 경우에만 보여줌
        if (rewardedAd != null) {
            rewardedAd?.show(this) { rewardItem ->
                // 보상 처리 (예: 포인트 지급)
//                Toast.makeText(applicationContext, "보상: ${rewardItem.amount}", Toast.LENGTH_SHORT).show()
                //post
                val phoneNumber = editPhoneNumber.text.toString().trim()
                sendRewards_ads(phoneNumber)
                //
                loadRewardedAd()
            }
        } else {
//           Toast.makeText(this, "리워드 광고가 로드되지 않았습니다.", Toast.LENGTH_SHORT).show()
        }
    }


//
//

    private fun loadRewardedInterstitialAd() {
        val adRequest = AdRequest.Builder().build()

        RewardedInterstitialAd.load(this, "ca-app-pub-3989916427250119/9331631930", adRequest,
            object : RewardedInterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedInterstitialAd) {
                    rewardedInterstitialAd = ad
                    Log.d("AdMob", "보상형 전면 광고 로드 완료")
//                    Toast.makeText(applicationContext, "보상형 전면 광고가 준비됨!", Toast.LENGTH_SHORT).show()
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    rewardedInterstitialAd = null
                    Log.e("AdMob", "보상형 전면 광고 로드 실패: ${loadAdError.code} - ${loadAdError.message}")
                }
            })
    }

    private fun showRewardedInterstitialAd() {
        if (rewardedInterstitialAd != null) {
            rewardedInterstitialAd!!.show(this) { rewardItem ->
                Log.d("AdMob", "사용자가 보상을 받음: ${rewardItem.amount} ${rewardItem.type}")
//                Toast.makeText(applicationContext, "보상 지급됨: ${rewardItem.amount} ${rewardItem.type}", Toast.LENGTH_SHORT).show()
                loadRewardedInterstitialAd()  // 광고 사용 후 새 광고 로드
            }
        } else {
            Log.d("AdMob", "광고가 아직 로드되지 않음")
//            Toast.makeText(applicationContext, "광고가 아직 준비되지 않았습니다.", Toast.LENGTH_SHORT).show()
        }
    }
//







//title: String, message: String, imageView: ImageView
    private fun showAlertDialog_menu(title: String, message: String,) {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.custom_dialog, null)
        builder.setView(dialogView)

        val viewPager = dialogView.findViewById<ViewPager2>(R.id.viewPager)
        val indicatorLayout = dialogView.findViewById<LinearLayout>(R.id.indicatorLayout)
        val button1 = dialogView.findViewById<Button>(R.id.popmenu_bt1)
        val button2 = dialogView.findViewById<Button>(R.id.popmenu_bt2)
        val closeButton = dialogView.findViewById<ImageButton>(R.id.closeButton)


        val messageView = dialogView.findViewById<TextView>(R.id.dialogMessage1)
        messageView.text = "현재 요청 가능한 잔여 횟수 : $message 번"

        val slides = listOf(
            SlideAdapter.SlideData("주의사항", "1. 주의사항 \n- 본 서비스는 좌석 확보를 보장하지 않습니다.\n- 출발 시간이 20분 이내인 열차는 예약할 수 없습니다.\n- 예약 조회는 30분 단위로 1개 좌석 예약 진행합니다.\n\n", R.drawable.sm_1 ),
//"1.현재 요청 가능한 잔여 횟수 : ${message} 번"

            SlideAdapter.SlideData("1. 무료 예약 요청 횟수(매일)",
                "2. 무료 예약 요청 횟수(매일)\n- 매일 00시에 기본 횟수 3회가 자동으로 충전됩니다.\n 무료사용자 : (30분) 무료 3회 기본 제공\n\n",R.drawable.sm_2),
            SlideAdapter.SlideData("3. 예약 요청 횟수 차감",     "3. 예약 요청 횟수 차감\n- 예약 요청 1회 시 잔여 횟수 1회가 차감됩니다.\n\n",R.drawable.sm_3),
            SlideAdapter.SlideData("4. 추가 예약 요청 충전", "4. 추가 예약 요청 충전\n" +
                    "- 광고 시청 또는 공유 시 예약 요청 1회 충전됩니다.\n" +
                    "- 일 최대 5회까지 추가 충전이 가능합니다.\n" +
                    "무료사용자 : (30분) 무료 3회 + 추가 충전 5회 = 총 8회\n",R.drawable.sm_4),
            SlideAdapter.SlideData("5. 예약 요청 횟수 초기화", "5. 예약 요청 횟수 초기화\n" +
                    "- 00시에 전일 잔여 예약 요청 횟수 전체 초기화됩니다.\n" + "- 00시가 되면 다시 기본 3장으로 초기화 됩니다.",R.drawable.sm_5),
            SlideAdapter.SlideData("6. 예약 시도 시간", "6. 예약 시도 시간\n" +
                    "- 예약 요청은 기입한 시간으로부터 \n 30분 이내 열차에 대해 최대 1시간동안 동작합니다.",R.drawable.sm_6)
            // 필요한 만큼 슬라이드 추가
        )

        val adapter = SlideAdapter(this, slides)
        viewPager.adapter = adapter
// 페이지 변경 감지
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateIndicators(indicatorLayout, position)
            }
        })

// 가로 방향 스크롤 설정
        viewPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL

        // 인디케이터 설정
        setUpIndicators(indicatorLayout, slides.size)
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateIndicators(indicatorLayout, position)
            }
        })
    val phoneNumber = editPhoneNumber.text.toString().trim()
    button1.setOnClickListener {
            showRewardedAd()
//            Toast.makeText(this, "팝업의 Button 1 클릭", Toast.LENGTH_SHORT).show()
            //시간 지연
            //기존창 끄기
            //새창 띄우기
//            val phoneNumber = editPhoneNumber.text.toString().trim()
//            sendReservationRequest_menu(phoneNumber)
        }

        button2.setOnClickListener {
            shareAppLink()
            sendRewards_ads(phoneNumber)
//            Toast.makeText(this, "팝업의 Button 2 클릭", Toast.LENGTH_SHORT).show()
        }

        val dialog = builder.create()

        dialog.setOnShowListener {
            val dialogViewPager = dialogView.findViewById<ViewPager2>(R.id.viewPager)
            dialogViewPager.layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.setCancelable(false)
        dialog.show()

        dialog.window?.apply {
            setLayout(
                (resources.displayMetrics.widthPixels * 0.95).toInt(),
                (resources.displayMetrics.heightPixels * 0.98).toInt()
            )
            setDimAmount(0.9f)
        }
    }

    private fun setUpIndicators(indicatorLayout: LinearLayout, count: Int) {
        val indicators = Array(count) { ImageView(this) }
        val layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(8, 0, 8, 0)
        for (i in indicators.indices) {
            indicators[i].apply {
                setImageDrawable(ContextCompat.getDrawable(this@MainActivity, R.drawable.indicator_inactive))
                this.layoutParams = layoutParams
            }
            indicatorLayout.addView(indicators[i])
        }
    }

    private fun updateIndicators(indicatorLayout: LinearLayout, position: Int) {
        for (i in 0 until indicatorLayout.childCount) {
            val imageView = indicatorLayout.getChildAt(i) as ImageView
            if (i == position) {
                imageView.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.indicator_active))
            } else {
                imageView.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.indicator_inactive))
            }
        }
    }









    // 공지사항을 서버에서 가져오는 함수
    private fun fetchNotices() {
        val apiService = retrofit.create(ApiService::class.java)
        apiService.getNotices()
            .enqueue(object : Callback<NoticeResponse> {
                override fun onResponse(call: Call<NoticeResponse>, response: Response<NoticeResponse>) {
                    if (response.isSuccessful) {
                        val notice = response.body()
                        if (notice != null) {
//                            Toast.makeText(this@MainActivity, "공지: ${notice.title}", Toast.LENGTH_SHORT).show()
                            showAlertDialog("공지사항", "${notice.title}\n\n${notice.content}")
                        } else {
                            showAlertDialog("공지사항", "현재 공지사항이 없습니다.")
                        }
                    } else {
                        val errorBody = response.errorBody()?.string() ?: "알 수 없는 오류"
                        showAlertDialog("공지사항 불러오기 실패", errorBody)
                    }
                }

                override fun onFailure(call: Call<NoticeResponse>, t: Throwable) {
                    Log.e("NetworkError", "공지사항 불러오기 실패: ${t.message}", t)
                    showAlertDialog("네트워크 오류", "공지사항을 불러오지 못했습니다. 다시 시도해주세요.")
                }
            })
    }



    // 예약 요청을 서버로 보내는 함수 - 잔여 횟수 확인을 위한 포스트
    private fun sendReservationRequest_menu(phone: String) {
//    val phone = editPhoneNumber.text.toString().trim()
        // 유효성 검사
        if (phone.isEmpty() ) {
            showAlertDialog("로그인", "휴대폰 정보를 입력하세요")
            return
        }

        val requestData_menu = mapOf(
            "phone_number" to phone
        )

        Log.d("RequestData", "보내는 데이터: $requestData_menu") // 요청 데이터 확인용 로그

        val apiService = retrofit.create(ApiService::class.java)
        apiService.sendReservation_menu(requestData_menu)
            .enqueue(object : Callback<ResponseData> {
                override fun onResponse(call: Call<ResponseData>, response: Response<ResponseData>) {
                    if (response.isSuccessful) {
                        val message = response.body()?.message ?: "성공적으로 처리되었습니다."
                        Log.d("ResponseData", "응답 데이터: ${response.body()}") // 응답 데이터 로그
//                        Toast.makeText(this@MainActivity, "예약 요청 성공: $message", Toast.LENGTH_SHORT).show()
                        showAlertDialog_menu("잔여 예약 요청 횟수", "$message") //"잔여 예약 요청 횟수", "$message",

                    } else {
                        // 서버에서 반환한 오류 메시지 로깅 및 표시
                        val errorBody = response.errorBody()?.string() ?: "알 수 없는 오류"
                        Log.e("ResponseError", "오류 응답: $errorBody")
//                        Toast.makeText(this@MainActivity, "예약 요청 실패: $errorBody", Toast.LENGTH_LONG).show()
                        showAlertDialog_menu("조회 실패", "$errorBody")
                    }

                }

                override fun onFailure(call: Call<ResponseData>, t: Throwable) {
                    // 네트워크 호출 실패 시 로그 및 Toast 출력
                    Log.e("NetworkError", "요청 실패: ${t.message}", t)
//                    Toast.makeText(this@MainActivity, "네트워크 오류: ${t.message}", Toast.LENGTH_LONG).show()
                }
            })
    }


    // 예약 요청을 서버로 보내는 함수
    private fun sendRewards_ads(phone: String) {
//    val phone = editPhoneNumber.text.toString().trim()
        // 유효성 검사
        if (phone.isEmpty() ) {
            showAlertDialog("로그인", "휴대폰 정보를 입력하세요")
            return
        }

        val requestData_menu = mapOf(
            "phone_number" to phone
        )

        Log.d("RequestData", "보내는 데이터: $requestData_menu") // 요청 데이터 확인용 로그

        val apiService = retrofit.create(ApiService::class.java)
        apiService.sendRewards(requestData_menu)
            .enqueue(object : Callback<ResponseData> {
                override fun onResponse(call: Call<ResponseData>, response: Response<ResponseData>) {
                    if (response.isSuccessful) {
                        val message = response.body()?.message ?: "성공적으로 처리되었습니다."
                        Log.d("ResponseData", "응답 데이터: ${response.body()}") // 응답 데이터 로그
//                        showAlertDialog("예약 요청 완료", "예약 요청이 완료되었습니다. 예약이 성공하면 푸시 알림이 발송됩니다.")
                    } else {
                        // 서버에서 반환한 오류 메시지 로깅 및 표시
                        val errorBody = response.errorBody()?.string() ?: "알 수 없는 오류"
                        Log.e("ResponseError", "오류 응답: $errorBody")
                    }
                }
                override fun onFailure(call: Call<ResponseData>, t: Throwable) {
                    // 네트워크 호출 실패 시 로그 및 Toast 출력
                    Log.e("NetworkError", "요청 실패: ${t.message}", t)
//                    Toast.makeText(this@MainActivity, "네트워크 오류: ${t.message}", Toast.LENGTH_LONG).show()
                }

            })
    }






    // 예약 요청을 서버로 보내는 함수
    private fun sendReservationRequest() {
        val phone = editPhoneNumber.text.toString().trim()
        val password = editPassword.text.toString().trim()
        val selectedPrivilege = privilege_spinner.selectedItem.toString().trim()
        val departure = spinnerDeparture.selectedItem.toString().trim()
        val arrival = spinnerArrival.selectedItem.toString().trim()
        val date = selectedDateValue
        val hh = spinnerHour.selectedItem.toString().trim() // 선택된 시간
        val mm = spinnerMinute.selectedItem.toString().trim() // 선택된 분

        // 유효성 검사
        if (phone.isEmpty() || password.isEmpty() || departure.isEmpty() || arrival.isEmpty() || date.isNullOrEmpty() || hh.isEmpty() || mm.isEmpty()) {
            showAlertDialog("입력 오류", "모든 필드를 입력해주세요.")
//            showInterstitialAd() // 전면 광고 표시
            return
        }

        if (departure == arrival) {
            showAlertDialog("입력 오류", "출발역과 도착역이 같을 수 없습니다.")
//            showInterstitialAd() // 전면 광고 표시
            return
        }
//fcm
        if (fcmToken == null) {
            showAlertDialog("FCM 오류", "푸시 알림 토큰을 가져올 수 없습니다. 잠시 후 다시 시도해주세요.")
            return
        }

        // 날짜 포맷 조정: "2024/12/05(목)" 형식 생성
        val formattedDate = date + "(" + getDayOfWeek(date) + ")"

        val requestData = mapOf(
            "phone_number" to phone,
            "password" to password,
            "departure" to departure,
            "arrival" to arrival,
            "date" to formattedDate,
            "hh" to hh, // 선택된 시간
            "mm" to mm, // 선택된 분
            "fcm_token" to fcmToken!!, // FCM 토큰 추가
            "privilege_type" to selectedPrivilege

        )

        Log.d("RequestData", "보내는 데이터: $requestData") // 요청 데이터 확인용 로그

        val apiService = retrofit.create(ApiService::class.java)
        apiService.sendReservation(requestData)
            .enqueue(object : Callback<ResponseData> {
                override fun onResponse(call: Call<ResponseData>, response: Response<ResponseData>) {
                    if (response.isSuccessful) {
                        val message = response.body()?.message ?: "성공적으로 처리되었습니다."
                        Log.d("ResponseData", "응답 데이터: ${response.body()}") // 응답 데이터 로그
                        showAlertDialog("예약 요청 완료", "예약이 성공하면 푸시 알림이 발송됩니다.")
//                        Toast.makeText(this@MainActivity, "예약 요청 성공: $message", Toast.LENGTH_SHORT).show()
//                        showInterstitialAd() // 전면 광고 표시
                        showRewardedInterstitialAd()
                        loadRewardedInterstitialAd()
                    } else {
                        // 서버에서 반환한 오류 메시지 로깅 및 표시
                        val errorBody = response.errorBody()?.string() ?: "알 수 없는 오류"
                        Log.e("ResponseError", "오류 응답: $errorBody")
//                        Toast.makeText(this@MainActivity, "예약 요청 실패: $errorBody", Toast.LENGTH_LONG).show()

                        // message 값 추출
                        val jsonObject = JSONObject(errorBody)
                        val message = jsonObject.optString("message", "알 수 없는 오류")
                        showAlertDialog("예약 요청 실패", "${message}")
                    }
//                    showInterstitialAd() // 전면 광고 표시
                    showRewardedInterstitialAd()
                    loadRewardedInterstitialAd()
                }

                override fun onFailure(call: Call<ResponseData>, t: Throwable) {
                    // 네트워크 호출 실패 시 로그 및 Toast 출력
                    Log.e("NetworkError", "요청 실패: ${t.message}", t)
//                    Toast.makeText(this@MainActivity, "네트워크 오류: ${t.message}", Toast.LENGTH_LONG).show()
//                    showInterstitialAd() // 전면 광고 표시
                    showRewardedInterstitialAd()
                    loadRewardedInterstitialAd()
                }
            })
    }

    // 요일 추출 함수
    private fun getDayOfWeek(date: String): String {
        val calendar = Calendar.getInstance()
        val sdf = java.text.SimpleDateFormat("yyyy/MM/dd")
        calendar.time = sdf.parse(date)!!
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            1 -> "일"
            2 -> "월"
            3 -> "화"
            4 -> "수"
            5 -> "목"
            6 -> "금"
            7 -> "토"
            else -> ""
        }
    }
}

interface ApiService {
    @POST("/reserve")
    fun sendReservation(@Body requestData: Map<String, String>): Call<ResponseData>

    @GET("/notices")
    fun getNotices(): Call<NoticeResponse>

    @POST("/remaining_attempts")
    fun sendReservation_menu(@Body requestData: Map<String, String>): Call<ResponseData>

    @POST("/plus_attempts")
    fun sendRewards(@Body requestData: Map<String, String>): Call<ResponseData>

}



data class NoticeResponse(
    val id: String,
    val title: String,
    val content: String,
    val version: String,
    val created_at: String
)

data class ResponseData(
    val success: Boolean,
    val message: String
)

