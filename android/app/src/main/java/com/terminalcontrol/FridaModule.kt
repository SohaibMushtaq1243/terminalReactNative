package com.terminalcontrol

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
//import io.ktor.client.HttpClient
//import io.ktor.client.plugins.websocket.WebSockets
//import io.ktor.client.plugins.websocket.webSocket
//import io.ktor.websocket.Frame
//import io.ktor.websocket.readText
import android.os.Debug
import android.view.View
import com.facebook.react.ReactPackage
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.uimanager.ReactShadowNode
import com.facebook.react.uimanager.ViewManager
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.io.BufferedReader
import java.io.InputStreamReader
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.OutputStreamWriter
import com.facebook.react.bridge.*
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import android.os.Build
import android.os.Bundle
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ObjectOutput
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Signature
import java.security.cert.X509Certificate
import java.security.spec.ECGenParameterSpec
import java.util.logging.Logger


@RequiresApi(Build.VERSION_CODES.M)
class FridaModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext),
    ReactPackage {
    private val executorService: ExecutorService = Executors.newSingleThreadExecutor()
    private val logger = Logger.getLogger("KtorWebsocketClient")
    override fun getName(): String {
        return "FridaModule"
    }

    @ReactMethod
    fun startFrida() {
        try {
            System.loadLibrary("frida-gadget")
            Debug.startNativeTracing()
            // Other Frida initialization if needed
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    @ReactMethod
    fun executeTerminalCommand(command: String, promise: Promise) {
        val parsedCommand = command.split(" ")
        val commandName = parsedCommand[0]
        val arguments = parsedCommand.drop(1)

        try {
            when (commandName) {
                "mkdir" -> {
                    val directory = File(arguments[0])
                    if (directory.mkdir()) {
                        promise.resolve("Directory created successfully")
                    } else {
                        promise.reject("Error creating directory")
                    }
                }
                "ls" -> {
                    val files = File(".").listFiles()
                        ?.joinToString("\n") { it.name }
                        ?: "Error listing files"
                    promise.resolve(files)
                }
//                "cd" -> {
//                    val directory = File(arguments[0])
//                    if (directory.exists() && directory.isDirectory) {
//                        File(".").canonicalFile = directory  // Change working directory
//                        promise.resolve("Directory changed successfully")
//                    } else {
//                        promise.reject("Error: Invalid directory")
//                    }
//                }
                else -> {
                    promise.reject("Unsupported command")
                }
            }
        } catch (e: Exception) {
            promise.reject(e)
        }
    }

    @ReactMethod
    fun executeCommand(command: String):String {
        try {
            val process = Runtime.getRuntime().exec(command)
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }
            reader.close()

            // Pass the output back to React Native (for demonstration purposes, you can send it to JavaScript)
            val finalOutput = output.toString()
            val mainHandler = Handler(Looper.getMainLooper())
            mainHandler.post {
                Toast.makeText(reactApplicationContext, finalOutput, Toast.LENGTH_LONG).show()
            }
            return finalOutput
        } catch (e: Exception) {
            e.printStackTrace()
            return e.toString();
        }
    }
    @ReactMethod
    fun handleAttestation(deviceId:String, nonce:String,callback: Callback) {
        print("deviceId"+deviceId);
        print("nonce:"+nonce)
        val gson = Gson()
        val mapAdapter = gson.getAdapter(object: TypeToken<Map<String, Any?>>() {})

        if (deviceId !==null  && nonce !== null && deviceId !==""  && nonce !== "") {
//            val deviceId: String = model["device_id"].toString()
            val challenge: ByteArray = Base64.decode(nonce, Base64.DEFAULT)

            val certs = try {
                // first try with StrongBox
                createAttestationKey(deviceId, challenge, true)
            } catch (e: Exception) {
//                e.toString();
                // fallback to StrongBox disabled
                createAttestationKey(deviceId, challenge, false)
            }
//            Log.d( "handleAttestation: ", gson.toJson(mapAdapter.toJson(hashMapOf("attestation" to certs))))
//            return mapAdapter.toJson(hashMapOf("attestation" to certs)).toString();
            callback(mapAdapter.toJson(hashMapOf("attestation" to certs)));
        } else {
            Log.d( "handleAttestation: ",mapAdapter.toJson(hashMapOf("error" to "invalid arguments!")))
            callback((mapAdapter.toJson(hashMapOf("error" to "invalid arguments!"))));
        }
    }
    @ReactMethod
//    fun handleSign(model: Map<String, Any?>): String {
//        val gson = Gson()
//        val mapAdapter = gson.getAdapter(object: TypeToken<Map<String, Any?>>() {})
//
//        if (model.containsKey("device_id") && model.containsKey("signature_inputs")) {
//            val res: MutableMap<String, String> = mutableMapOf()
//            val deviceId: String = model["device_id"].toString()
//            val signatureInputs: Map<String, String> = model["signature_inputs"] as Map<String, String>
//            for (entry in signatureInputs.entries) {
//                val signature = sign(deviceId, entry.value)
//                res[entry.key] = signature
//            }
//            return mapAdapter.toJson(res)
//        } else {
//            return mapAdapter.toJson(hashMapOf("error" to "invalid arguments!"))
//        }
//    }
    fun handleSign(deviceId:String,signatureinputs:String,callback: Callback) {
       try{
           Log.d("signatureinputs--------",signatureinputs);
           val gson = Gson()
           val mapAdapter = gson.getAdapter(object: TypeToken<Map<String, Any?>>() {})

           if (deviceId !== null && signatureinputs !== null) {
               val res: MutableMap<String, String> = mutableMapOf()
//            val signatureInputs: Map<String, String> = signatureinputs as Map<String, String>
//            for (entry in signatureInputs.entries) {
//                val signature = sign(deviceId, entry.value)
//                res[entry.key] = signature
//            }
               val signature = sign(deviceId, signatureinputs)
               Log.d("signature-----",signature);
//               res["/ahs"] = signature
               callback(signature);
           } else {
               callback(hashMapOf("error" to "invalid arguments!"));
           }
       }catch(e :Exception){
           Log.d( "handleAttestation: ",e.toString())
       }
    }
    private fun sign(deviceId: String, signatureInput: String): String {
        val alias = "_amazon_attestation_key_$deviceId"
        Log.d("alias",alias);
        return try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore").also {
                it.load(null, null)
            }
            val key = keyStore.getKey(alias, null) as PrivateKey
            val signatureInstance = Signature.getInstance("SHA256withECDSA")
            signatureInstance.initSign(key)
            signatureInstance.update(signatureInput.toByteArray())
            Base64.encodeToString(signatureInstance.sign(), Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.d("Signing error:",e.toString())
            logger.warning("Signing error: ${e.localizedMessage}")
            ""
        }
    }
    private fun createAttestationKey(deviceId: String, challenge: ByteArray, enableStrongBox: Boolean): ArrayList<String> {
        val certs = ArrayList<String>()
        val alias = "_amazon_attestation_key_$deviceId"
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_SIGN)
                .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                .setAttestationChallenge(challenge)
                .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
        } else {
            TODO("VERSION.SDK_INT < N")
        }

        if (enableStrongBox && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            builder.setIsStrongBoxBacked(true)
        }

        val keyStore = KeyStore.getInstance("AndroidKeyStore").also {
            it.load(null, null)
        }
        val keyPairGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, keyStore.provider).also {
            it.initialize(builder.build())
        }
        keyPairGenerator.generateKeyPair()
        val certificates = keyStore.getCertificateChain(alias)
        for (cert in certificates) {
            val b64Encoded = Base64.encodeToString((cert as X509Certificate).encoded, Base64.NO_WRAP)
            certs.add(b64Encoded)
        }
        return certs
    }

    override fun createNativeModules(p0: ReactApplicationContext): MutableList<NativeModule> {
        TODO("Not yet implemented")
    }

    override fun createViewManagers(p0: ReactApplicationContext): MutableList<ViewManager<View, ReactShadowNode<*>>> {
        TODO("Not yet implemented")
    }
    // Other methods to interact with Frida using frida-node
}
