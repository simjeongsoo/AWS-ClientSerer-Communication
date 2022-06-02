package com.example.mobile_programming

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import okhttp3.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException


//==================================================================================================
//class start
class MainActivity : AppCompatActivity() {

    private val TAG = "#######################"

    //startActivityForResult(Intent intent, int requestCode)를 위한 변수 (activity 식별용)
    private val PICK_FROM_ALBUM = 1

    private var tempFile: File? = null

    private var originalBm: Bitmap? = null

    var apiService: ApiService? = null

    private var filenamesave: File? = null

    //==================================================================================================
// onCreate start
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
            Context.MODE_PRIVATE)
    /*--------------------------------------------------------------------------------------------------*/

        val actionBar = supportActionBar
        actionBar!!.hide()
    /*--------------------------------------------------------------------------------------------------*/

        initRetrofitClient()

    /*--------------------------------------------------------------------------------------------------*/

        //IMGPIC 버튼 클릭시 동작
        findViewById<View>(R.id.btnGallery).setOnClickListener {
                goToAlbum()
        }

    /*--------------------------------------------------------------------------------------------------*/

        //IMAGEUP 버튼 클릭시 동작
        findViewById<View>(R.id.btnUpload).setOnClickListener {
            multipartImageUpload()
        }

    }// onCreate end


//==================================================================================================
    //   initRetrofitClient Start
    private fun initRetrofitClient() {
        val client = OkHttpClient.Builder().build()
        apiService = Retrofit.Builder()
            .baseUrl("http://ec2-13-125-162-100.ap-northeast-2.compute.amazonaws.com:3000")
            .client(client).build().create(ApiService::class.java)
    }
//==================================================================================================


    //==================================================================================================
    // goToAlbum start
    /**
     * 앨범에서 이미지 가져오기
     */
    private fun goToAlbum() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = MediaStore.Images.Media.CONTENT_TYPE
        startActivityForResult(intent, PICK_FROM_ALBUM)
    } //goToAlbum end

    //==================================================================================================

    // onActivityResult Start
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode != RESULT_OK) {
            Log.d(TAG, "onActivityResult에서 resultCode != Activity.RESULT_OK라면 : 실행취소 ")
            Toast.makeText(this, "취소 되었습니다.", Toast.LENGTH_SHORT).show()

            if (tempFile != null) {
                if (tempFile!!.exists()) {
                    if (tempFile!!.delete()) {
                        Log.e(TAG, tempFile!!.getAbsolutePath() + " 삭제 성공")
                        tempFile = null
                    }
                }
            }
            return
        }


        if (requestCode == PICK_FROM_ALBUM) {
            Log.d(TAG, "onActivityResult에서 requestCode == PICK_FROM_ALBUM라면 : 실행 ")

            // 가져온 데이터 값을 Uri 값으로 변환 -> content// 값 ( 경로로 쓸수 없음 절대경로로 변환 해야함 )
            val photoUri = data!!.data
            Log.d(TAG, "PICK_FROM_ALBUM photoUri : $photoUri")

            var cursor: Cursor? = null
            try {
                /*
                 *  Uri 스키마를
                 *  content:/// 에서 file:/// 로  변경한다.
                 */
                val proj = arrayOf(MediaStore.Images.Media.DATA)
                Log.d(TAG, "onActivityResult proj  : $proj")

                assert(photoUri != null)
                cursor = contentResolver.query(photoUri!!, proj, null, null, null)
                Log.d(TAG, "onActivityResult curour  : $cursor")
                assert(cursor != null)

                val column_index = cursor!!.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                Log.d(TAG, "onActivityResult  column_index : $column_index")

                cursor.moveToFirst()
                tempFile = File(cursor.getString(column_index))
                Log.d(TAG, "tempFile Uri : " + Uri.fromFile(tempFile))
                Log.d(TAG, "tempFile : " + tempFile)

                filenamesave = tempFile

            } finally {
                cursor?.close()
            }
            setImage()
        }
    } // onActivityResult end

    //==================================================================================================
    //  setImage Start
    /**
     * tempFile 을 bitmap 으로 변환 후 ImageView 에 설정한다.
     */
    private fun setImage() {
        val imageView = findViewById<ImageView>(R.id.imageView)

        //bitmapFactory
        val options = BitmapFactory.Options()

        originalBm = BitmapFactory.decodeFile(tempFile!!.absolutePath, options)

        // tempFile 에서  absolutePath 주소 앞에있던 file:// delete
        Log.d(TAG, "setImage 에서 tempFile.absolutePath값 :  " + tempFile!!.absolutePath)

        imageView.setImageBitmap(originalBm)
        /**
         * tempFile 사용 후 null 처리를 해줘야 합니다.
         * (resultCode != RESULT_OK) 일 때 tempFile 을 삭제하기 때문에
         * 기존에 데이터가 남아 있게 되면 원치 않은 삭제가 이뤄집니다.
         */
        tempFile = null
    } // setImage end


    //==================================================================================================
    // multipartImageUpload start

    private fun multipartImageUpload() {
        try {
            /*
            filenamesave = /storage/emulated/0/DCIM/Camera/파일명.확장자
            */

            val reqFile = RequestBody.create(MediaType.parse("image/*"), filenamesave)
            val body = MultipartBody.Part.createFormData("upload", filenamesave!!.name, reqFile)
            val name = RequestBody.create(MediaType.parse("text/plain"), "upload")


            // 여기가 전송부분 apiService 인터페이스에 선언해놓은 post 방식으로 이미지를 보냄
            val req = apiService!!.postImage(body, name)

            //이후 처리 부분
            req!!.enqueue(object : Callback<ResponseBody?> {
                override fun onResponse(call: Call<ResponseBody?>, response: Response<ResponseBody?>) {
                    Toast.makeText(applicationContext, "Uploaded Successfully!", Toast.LENGTH_SHORT).show()
                }

                override fun onFailure(call: Call<ResponseBody?>, t: Throwable) {
                    Toast.makeText(applicationContext, "Request failed", Toast.LENGTH_SHORT).show()
                    t.printStackTrace()
                }
            })

        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }// multipartImageUpload end

}//class end