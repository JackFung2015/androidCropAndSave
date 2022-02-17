package com.zy.androidcrop

import android.content.ClipData
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.zy.androidcrop.ext.toast
import com.zy.androidcrop.utils.CropFileUtils
import java.io.File

class MainActivity : AppCompatActivity() {
    private val headImg: ImageView by lazy {
        findViewById(R.id.headImg)
    }

    /**
     * 7.0获取的图片地址，与7.0之前方式不一样
     */
    private var takePhotoSaveAdr: Uri? = null

    /**
     * 裁剪图片的的地址，最终加载它
     * 用于拍照完成或者选择本地图片之后
     */
    private var uriClipUri: Uri? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<Button>(R.id.galleryBtn).setOnClickListener {
            getRequirePermission(VALUE_GO_GALLERY)
        }
        findViewById<Button>(R.id.cameraBtn).setOnClickListener {
            getRequirePermission(VALUE_GO_CAMERA)
        }
    }

    /***
     * 获取读写权限
     */
    private fun getRequirePermission(flag: Int) {
        XXPermissions.with(this)
                .permission(Permission.Group.STORAGE)
                .request(object : OnPermissionCallback {
                    override fun onGranted(permissions: MutableList<String>?, all: Boolean) {
                        if (all) {
                            if (flag == VALUE_GO_GALLERY) {
                                openGallery()
                            } else {
                                openCamera()
                            }
                        } else {
                            toast("获取部分权限成功，但部分权限未正常授予")
                        }
                    }

                    override fun onDenied(permissions: MutableList<String>?, never: Boolean) {
                        super.onDenied(permissions, never)
                        if (never) {
                            toast("被永久拒绝授权，请手动授予权限")
                            // 如果是被永久拒绝就跳转到应用权限系统设置页面
                            XXPermissions.startPermissionActivity(this@MainActivity, permissions)
                        } else {
                            toast("获取读写权限失败")
                        }
                    }
                })
    }

    /***
     * 使用相机
     */
    private fun openCamera() {
        TAKEPAHTO = 1
        // 启动系统相机
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val mImageCaptureUri: Uri
        // 判断7.0android系统
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            //临时添加一个拍照权限
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            //通过FileProvider获取uri
            takePhotoSaveAdr = FileProvider.getUriForFile(this@MainActivity,
                    "com.zy.androidcrop", File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "head.jpg"))
            intent.putExtra(MediaStore.EXTRA_OUTPUT, takePhotoSaveAdr)
        } else {
            mImageCaptureUri = Uri.fromFile(File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "head.jpg"))
            intent.putExtra(MediaStore.EXTRA_OUTPUT, mImageCaptureUri)
        }
        startActivityForResult(intent, PHOTO_TAKEPHOTO)
    }

    /***
     * 使用图库
     */
    private fun openGallery() {
        TAKEPAHTO = 0
        val intentAlbum = Intent(Intent.ACTION_PICK, null)
        //使用INTERNAL_CONTENT_URI只能显示存储在内部的照片
        intentAlbum.setDataAndType(
                MediaStore.Images.Media.INTERNAL_CONTENT_URI, "image/*")
        //返回结果和标识
        startActivityForResult(intentAlbum, PHOTO_PHOTOALBUM)
    }

    var imageCropFile: File? = null

    /**
     * 图片裁剪的方法
     * @param uri
     */
    private fun startPhotoZoom(uri: Uri) {
        Log.e("uri=====", "" + uri)
        val intent = Intent("com.android.camera.action.CROP")//com.android.camera.action.CROP，这个action是调用系统自带的图片裁切功能
        intent.setDataAndType(uri, "image/*") //裁剪的图片uri和图片类型
        intent.putExtra("crop", "true") //设置允许裁剪，如果不设置，就会跳过裁剪的过程，还可以设置putExtra("crop", "circle")
        intent.putExtra("aspectX", 1) //裁剪框的 X 方向的比例,需要为整数
        intent.putExtra("aspectY", 1) //裁剪框的 Y 方向的比例,需要为整数
        intent.putExtra("outputX", 500) //返回数据的时候的X像素大小。
        intent.putExtra("outputY", 500) //返回数据的时候的Y像素大小。
        //uriClipUri为Uri类变量，实例化uriClipUri
        //android11 分区存储
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            imageCropFile = CropFileUtils.createImageFile(this, true)
            //设置裁剪的图片地址Uri
            uriClipUri = CropFileUtils.uri
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (TAKEPAHTO == 1) { //如果是7.0的拍照
                //开启临时访问的读和写权限
                intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                //针对7.0以上的操作
                intent.clipData = ClipData.newRawUri(MediaStore.EXTRA_OUTPUT, uri)
                uriClipUri = uri
            } else { //如果是7.0的相册
                //设置裁剪的图片地址Uri
                uriClipUri = Uri.parse("file://" + "/" + getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.path + "/" + "head.jpg")
            }
        } else {
            uriClipUri = Uri.parse("file://" + "/" + getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.path + "/" + "head.jpg")
        }
        Log.e("uriClipUri=====", "" + uriClipUri)
        //Android 对Intent中所包含数据的大小是有限制的，一般不能超过 1M，否则会使用缩略图 ,所以我们要指定输出裁剪的图片路径
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uriClipUri)
        intent.putExtra("return-data", false) //是否将数据保留在Bitmap中返回
        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString()) //输出格式，一般设为Bitmap格式及图片类型
        intent.putExtra("noFaceDetection", false) //人脸识别功能
        startActivityForResult(intent, PHOTO_PHOTOCLIP) //裁剪完成的标识
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                PHOTO_TAKEPHOTO -> {
                    //判断如果是7.0
                    val clipUri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        takePhotoSaveAdr!!
                    } else {
                        Uri.fromFile(File(Environment.getExternalStorageDirectory().toString() + "/photo.jpg"))
                    }
                    //获取拍照结果，执行裁剪
                    startPhotoZoom(clipUri)
                }
                PHOTO_PHOTOALBUM -> {
                    //获取图库结果，执行裁剪
                    data!!.data?.let { startPhotoZoom(it) }
                }
                PHOTO_PHOTOCLIP -> {
                    //裁剪完成后的操作，上传至服务器或者本地设置
                    val optionsa = RequestOptions()
                    optionsa.placeholder(R.mipmap.ic_launcher)
                    optionsa.error(R.mipmap.ic_launcher_round) //异常显示图
                    optionsa.diskCacheStrategy(DiskCacheStrategy.NONE) //禁用掉Glide的缓存功能
                    optionsa.skipMemoryCache(true) //禁用掉Glide的内存缓存
                    //显示页面上
                    if (imageCropFile != null && imageCropFile!!.absolutePath != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            if (CropFileUtils.uri != null) {
                                // 通过存储的uri 查询File
                                imageCropFile = CropFileUtils.getCropFile(this, CropFileUtils.uri)
                                Glide.with(this).load(CropFileUtils.uri).apply(optionsa).into(headImg)
                            }
                        } else {
                            Glide.with(this).load(uriClipUri).apply(optionsa).into(headImg)
                        }
                    } else {
                        Glide.with(this).load(uriClipUri).apply(optionsa).into(headImg)
                    }
                }
            }
        }
    }

    companion object {
        const val VALUE_GO_GALLERY = 1//图库
        const val VALUE_GO_CAMERA = 2//相册
    }

    //图库
    private val PHOTO_PHOTOALBUM = 0

    //拍照
    private val PHOTO_TAKEPHOTO = 1

    //裁剪
    private val PHOTO_PHOTOCLIP = 2

    //图片拍照的标识,1拍照0相册
    private var TAKEPAHTO = 1
}