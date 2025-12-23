package com.example.ukonnect2.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ukonnect2.R
import com.example.ukonnect2.network.LoginRequest
import com.example.ukonnect2.network.LoginResponse
import com.example.ukonnect2.network.RetrofitClient
import com.example.ukonnect2.network.SessionManager
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val orangeColor = Color(0xFFFF9800)
    val blackColor = Color(0xFF000000)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            Image(
                painter = painterResource(id = R.drawable.ic_logo),
                contentDescription = "Logo UKOnnect",
                modifier = Modifier.size(110.dp)
            )

            Text(
                "Login UKOnnect",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = orangeColor
            )

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = orangeColor,
                    unfocusedBorderColor = blackColor
                ),
                modifier = Modifier.fillMaxWidth(0.85f)
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            painter = painterResource(
                                id = if (passwordVisible) R.drawable.ic_eye_open
                                else R.drawable.ic_eye_closed
                            ),
                            contentDescription = null,
                            tint = if (passwordVisible) orangeColor else blackColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = orangeColor,
                    unfocusedBorderColor = blackColor
                ),
                modifier = Modifier.fillMaxWidth(0.85f)
            )

            Button(
                onClick = {
                    if (username.isNotEmpty() && password.isNotEmpty()) {
                        isLoading = true
                        errorMessage = ""

                        val request = LoginRequest(username, password)
                        RetrofitClient.instance.login(request)
                            .enqueue(object : Callback<LoginResponse> {
                                override fun onResponse(
                                    call: Call<LoginResponse>,
                                    response: Response<LoginResponse>
                                ) {
                                    isLoading = false
                                    if (response.isSuccessful && response.body() != null) {
                                        val body = response.body()!!
                                        if (body.message.contains("berhasil", ignoreCase = true)) {
                                            val token = body.token
                                            val userId = body.userId
                                            if (token.isNullOrBlank() || userId == null) {
                                                errorMessage = "Login berhasil tapi token tidak ada. Cek backend."
                                                return
                                            }
                                            SessionManager.setSession(token, userId)
                                            onLoginSuccess()
                                        } else {
                                            errorMessage = body.message
                                        }
                                    } else {
                                        errorMessage = "Login gagal: ${response.message()}"
                                    }
                                }

                                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                                    isLoading = false
                                    errorMessage = "Tidak bisa terhubung ke server: ${t.message}"
                                }
                            })
                    } else {
                        errorMessage = "Isi username dan password!"
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = orangeColor),
                modifier = Modifier
                    .width(200.dp)
                    .height(48.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text("Login", fontSize = 17.sp, color = Color.White)
                }
            }

            if (errorMessage.isNotEmpty()) {
                Text(errorMessage, color = Color.Red, fontSize = 13.sp)
            }
        }
    }
}
