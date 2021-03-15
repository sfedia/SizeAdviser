package com.sizeadviser.sizeadviser

import android.content.SharedPreferences
import android.util.Log
import androidx.preference.PreferenceManager
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FileDataPart
import com.github.kittinunf.result.Result
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.gson.Gson
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.pow
import kotlin.random.Random

data class RegisterNewAccountResult(
    val code: String,
    val status: String
)

data class RandomBrand(
    val brand: String? = null
)

data class BrandData(
    val standards: List<BrandDataStandard>? = null,
    val defaultStandard: String? = null
)

data class BrandDataStandard(
    val standard: String? = null,
    val sizes: List<String>? = null
)

data class RecommendedSizes(
    val recommendations: List<SizeObject>? = null
) {
    fun forStandard(st: String) : String? {
        recommendations?.forEach {
            if (it.standard == st) {
                return it.value
            }
        }
        return null
    }
}

data class SizeObject(
    val standard: String? = null,
    val value: String? = null
)

data class AllBrands(
    val listBrands: List<String>? = null
)

data class BoundFittingData(
    var standardsObject: BrandData? = null,
    var recommended: RecommendedSizes? = null,
    var brandOptions: AllBrands? = null,
    var thisBrand: String? = null
)

data class StandardSizeUnit(
    val standard: String? = null,
    val size: String? = null
)

data class SelectiveProfileBrandRow(
    val brand: String? = null,
    val SSUnit: StandardSizeUnit? = null,
    val triedOn: Boolean? = null
)

data class ProfileBrandRow(
    val brand: String? = null,
    val systemsOfSize: List<StandardSizeUnit>? = null,
    val triedOn: Boolean? = null
)

data class DataForGender(
    val data: List<ProfileBrandRow>? = null
)

data class CollectionItem(
    val brand: String? = null,
    val standard: String? = null,
    val size: String? = null,
    val fit_value: Int? = null,
    val fittingID: String? = null,
    val date: String? = null,
    val has_photos: Boolean? = null,
    val is_empty: Boolean = false
)


data class MyCollectionData(
    val items: List<CollectionItem>? = null
)

data class ImageUploaded(
    val uploaded: Boolean,
    val result: String
)


class SizeAdviserApi {

    var user: FirebaseUser = FirebaseAuth.getInstance().currentUser!!
    var firebaseBasicUrl: String = "https://size-adviser.com/firebase/"
    var mobileBasicUrl: String = "https://size-adviser.com/mobile/"

    init {
        println("api instance started")
    }

    fun registerCurrentUser(
        sharedPreferences: SharedPreferences, times: Int = 1, update: Boolean = false) {
        val queryArgs: MutableList<Pair<String, String?>> = mutableListOf(
            "firebase_uid" to user.uid,
            "user_email" to user.email,
            "user_name" to user.displayName,
            "user_gender" to sharedPreferences.getString(
                "profile_gender", "0"
            ).toString()
        )
        if (update)
            queryArgs.add("rewrite" to "1");
        Fuel.get(firebaseBasicUrl + "register_new_account", queryArgs)
            .response { result ->
                when (result) {
                    is Result.Failure -> {
                        if (times < 3) {
                            registerCurrentUser(sharedPreferences, times)
                        }
                    }
                    else -> {}
                }
            }
    }

    fun boundLoadFittingData(
        sharedPreferences: SharedPreferences, brand: String?,
        processBoundData: (BoundFittingData) -> Unit) {
        var reqDone: Int = 0
        val bound = BoundFittingData()

        if (brand == null) {
            Fuel.get(mobileBasicUrl + "random_brand",
                listOf("gender_int" to sharedPreferences.getString(
                    "profile_gender", "0"
                ).toString()))
                .responseString { _, _, result ->
                    when (result) {
                        is Result.Success -> {
                            boundLoadFittingData(sharedPreferences,
                                Gson().fromJson(
                                    result.get(), RandomBrand::class.java
                                ).brand,
                                processBoundData
                            )
                        }
                        else -> return@responseString
                    }
                }
            return
        }

        bound.thisBrand = brand

        Fuel.get(mobileBasicUrl + "get_brand_data", listOf(
            "brand" to brand,
            "gender_int" to sharedPreferences.getString(
                "profile_gender", "0"
            ).toString()
        ))
            .responseString { _, response, result ->
                when (result) {
                    is Result.Failure -> {
                        println(response)
                    }
                    is Result.Success -> {
                        val brandSt = Gson().fromJson(
                            result.get(), BrandData::class.java
                        )
                        bound.standardsObject = brandSt
                        reqDone++
                        Log.d("reqDone", reqDone.toString())
                        if (reqDone == 3)
                            processBoundData(bound);
                    }}
                }

        Fuel.get(mobileBasicUrl + "recommended_size", listOf(
            "brand" to brand,
            "gender_int" to sharedPreferences.getString(
                "profile_gender", "0"
            ).toString(),
            "user_id" to user.uid
        ))
            .responseString { _, response, result ->
                when (result) {
                    is Result.Failure -> {
                        println(response)
                    }
                    is Result.Success -> {
                        val recSizes = Gson().fromJson(
                            result.get(), RecommendedSizes::class.java
                        )
                        bound.recommended = recSizes
                        reqDone++
                        if (reqDone == 3)
                            processBoundData(bound);
                    }}
            }

        Fuel.get(mobileBasicUrl + "get_brands", listOf(
            "gender_int" to sharedPreferences.getString(
                "profile_gender", "0"
            ).toString()
        ))
            .responseString { _, _, result ->
                when (result) {
                    is Result.Failure -> {
                        println("Error")
                    }
                    is Result.Success -> {
                        val resBrands = Gson().fromJson(
                            result.get(), AllBrands::class.java
                        )
                        bound.brandOptions = resBrands
                        reqDone++
                        if (reqDone == 3)
                            processBoundData(bound);
                    }}
            }
    }

    fun tryWithSize(fittingID: String, brand: String,
                    size: String, standard: String, fitValue: Int, hook: () -> Unit) {

        val date = Calendar.getInstance().time
        val formatter = SimpleDateFormat("HH.mm.ss.dd.MM.yyyy")
        Fuel.get(mobileBasicUrl + "try_with_size", listOf(
            "user_id" to user.uid,
            "fitting_id" to fittingID,
            "brand" to brand,
            "size" to size,
            "system" to standard,
            "fit_value" to fitValue,
            "date" to formatter.format(date)
        ))
            .responseString { _, _, result ->
                when (result) {
                    is Result.Failure -> {
                        println("FAILURE")
                    }
                    is Result.Success -> {
                        println("success")
                        hook()
                    }}
            }
    }

    fun getDataForUser(sharedPreferences: SharedPreferences,
                       processData: (DataForGender) -> Unit) {
        Fuel.get(mobileBasicUrl + "data_for_gender", listOf(
            "user_id" to user.uid,
            "gender_int" to sharedPreferences.getString(
                "profile_gender", "0"
            ).toString()
        ))
            .responseString { _, _, result ->
                when (result) {
                    is Result.Failure -> {
                        println("Error")
                    }
                    is Result.Success -> {
                        val dataForUser = Gson().fromJson(
                            result.get(), DataForGender::class.java
                        )
                        processData(dataForUser);
                    }}
            }
    }

    fun getCollection(sharedPreferences: SharedPreferences,
                      processData: (MyCollectionData) -> Unit) {
        Fuel.get(mobileBasicUrl + "get_collection_items", listOf(
            "user_id" to user.uid
        ))
            .responseString { _, _, result ->
                when (result) {
                    is Result.Failure -> {
                        println("Error")
                    }
                    is Result.Success -> {
                        println(result.get())
                        val myCollection = Gson().fromJson(
                            result.get(), MyCollectionData::class.java
                        )
                        processData(myCollection)
                    }
                }
            }
    }

    fun uploadPhoto(fittingID: String, photoID: String, f: File,
                onPhotoUploaded: (ImageUploaded) -> Unit) {
        Fuel.upload("$mobileBasicUrl${user.uid}/$fittingID/$photoID/upload_photo")
            .add(
                FileDataPart(f, name = "file", filename="photo_$photoID.png")
            ).responseString { _, _, result ->
                when (result) {
                    is Result.Failure -> {
                        println("Error")
                    }
                    is Result.Success -> {
                        val output = Gson().fromJson(
                            result.get(), ImageUploaded::class.java
                        )
                        onPhotoUploaded(output)
                    }
                }
            }
    }

    fun getItemPhotoURL(fittingID: String, index: Int = 0, thumbnail: Boolean = false): String {
        val antiCacheParam = Random.nextInt(
            10.toDouble().pow(6).toInt(),
            10.toDouble().pow(8).toInt()
        ).toString()
        var url = "${mobileBasicUrl}get_images?index=$index&fitting_id=$fittingID&c=$antiCacheParam"
        if (thumbnail) {
            url += "&thumbnail=yes"
        }
        return url
    }

    fun getAllStandards(): List<String> {
        return listOf("US", "UK", "RU", "Cm", "EU")
    }

    fun removeFitting(fittingID: String, onFittingRemoved: () -> Unit) {
        Fuel.get(mobileBasicUrl + "remove_collection_item", listOf(
            "user_id" to user.uid,
            "fitting_id" to fittingID
        ))
            .responseString { _, _, result ->
                when (result) {
                    is Result.Failure -> {
                        println("Error")
                    }
                    is Result.Success -> {
                        onFittingRemoved()
                    }
                }
            }
    }

    fun removePhoto(fittingID: String, photoIndex: Int, onPhotoRemoved: () -> Unit) {
        Fuel.get(mobileBasicUrl + "remove_photo_by_index", listOf(
            "user_id" to user.uid,
            "fitting_id" to fittingID,
            "photo_index" to photoIndex.toString()
        ))
            .responseString {_, _, result ->
                when (result) {
                    is Result.Success -> {
                        onPhotoRemoved()
                    }
                    is Result.Failure -> {}
                }
            }
    }
}

