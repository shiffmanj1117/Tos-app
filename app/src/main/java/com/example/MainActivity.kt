package com.example

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Bundle
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MaterialTheme(
        colorScheme = darkColorScheme(
          primary = Color(0xFF66FCF1),
          secondary = Color(0xFF45A29E),
          tertiary = Color(0xFFC5C6C7),
          background = Color(0xFF0B0C10),
          surface = Color(0xFF1F2833)
        )
      ) {
        TommiOsApp()
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TommiOsApp() {
  val context = LocalContext.current
  val sharedPref = remember { context.getSharedPreferences("tommi_os_prefs", Context.MODE_PRIVATE) }

  // Dynamic permission launcher for Camera, Mic, and Location services
  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions()
  ) { _ -> }

  LaunchedEffect(Unit) {
    permissionLauncher.launch(
      arrayOf(
        android.Manifest.permission.CAMERA,
        android.Manifest.permission.RECORD_AUDIO,
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_COARSE_LOCATION
      )
    )
  }
  
  // Persist customized URLs across app restarts so the user has an operational utility
  var targetUrl by remember { 
    mutableStateOf(sharedPref.getString("target_url", "https://tommi-os.local:3000") ?: "https://tommi-os.local:3000") 
  }
  
  var isError by remember { mutableStateOf(false) }
  var isLoading by remember { mutableStateOf(true) }
  var loadProgress by remember { mutableStateOf(0) }
  var webViewInstance by remember { mutableStateOf<WebView?>(null) }
  var showUrlConfigDialog by remember { mutableStateOf(false) }
  var tempUrlInput by remember { mutableStateOf(targetUrl) }

  // Handle Android back navigation inside WebView back-stack
  BackHandler(enabled = webViewInstance?.canGoBack() == true && !isError) {
    webViewInstance?.goBack()
  }

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    topBar = {
      // Small decorative top status bar matching the cyberpunk theme
      CenterAlignedTopAppBar(
        title = {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
          ) {
            Text(
              text = "TOMMI OS",
              color = Color(0xFF66FCF1),
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 2.sp,
              fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.width(8.dp))
            // Blinking status dot (green if connected/loading, pulsing red if error)
            StatusIndicatorDot(isError = isError)
          }
        },
        actions = {
          IconButton(
            onClick = { 
              tempUrlInput = targetUrl
              showUrlConfigDialog = true 
            },
            modifier = Modifier.testTag("settings_button")
          ) {
            Icon(
              imageVector = Icons.Default.Settings,
              contentDescription = "Configure connection URL",
              tint = Color(0xFF66FCF1)
            )
          }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
          containerColor = Color(0xFF0B0C10),
          titleContentColor = Color(0xFF66FCF1)
        )
      )
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color(0xFF0B0C10))
        .padding(innerPadding)
    ) {
      // WebView Core Layer
      AndroidView(
        factory = { ctx ->
          WebView(ctx).apply {
            layoutParams = android.view.ViewGroup.LayoutParams(
              android.view.ViewGroup.LayoutParams.MATCH_PARENT,
              android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
            settings.apply {
              javaScriptEnabled = true
              domStorageEnabled = true
              databaseEnabled = true
              loadWithOverviewMode = true
              useWideViewPort = true
              cacheMode = WebSettings.LOAD_DEFAULT
              mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
              allowFileAccess = true
              allowContentAccess = true
            }
            
            webViewClient = object : WebViewClient() {
              private var hasError = false

              override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                hasError = false
                isLoading = true
                isError = false
              }

              override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                isLoading = false
                // Keep the error screen visible if an error occurred
                isError = hasError
              }

              override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
              ) {
                super.onReceivedError(view, request, error)
                // Filter out non-main frame resources (analytics/ads) triggering false errors
                if (request?.isForMainFrame == true) {
                  hasError = true
                  isError = true
                }
              }

              @SuppressLint("WebViewClientOnReceivedSslError")
              override fun onReceivedSslError(
                view: WebView?,
                handler: SslErrorHandler?,
                error: SslError?
              ) {
                // Since this points to a local network .local domain or private IP, 
                // proceed gracefully to tolerate custom or self-signed development certificates.
                handler?.proceed()
              }
            }

            webChromeClient = object : WebChromeClient() {
              override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                loadProgress = newProgress
              }

              override fun onPermissionRequest(request: PermissionRequest?) {
                // Grant all requested media or sensor permissions to the web page
                request?.grant(request.resources)
              }

              override fun onGeolocationPermissionsShowPrompt(
                origin: String?,
                callback: GeolocationPermissions.Callback?
              ) {
                // Allow geolocation prompts automatically for the local domain
                callback?.invoke(origin, true, false)
              }
            }
            
            webViewInstance = this
            loadUrl(targetUrl)
          }
        },
        update = { webView ->
          // Reload if the target URL has changed externally
          if (webView.url != targetUrl && !isError && !isLoading) {
            webView.loadUrl(targetUrl)
          }
        },
        modifier = Modifier
          .fillMaxSize()
          .testTag("webview_container")
      )

      // Top Loading Linear Indicator
      if (isLoading && !isError) {
        LinearProgressIndicator(
          progress = { loadProgress / 100f },
          modifier = Modifier
            .fillMaxWidth()
            .height(3.dp)
            .align(Alignment.TopCenter),
          color = Color(0xFF66FCF1),
          trackColor = Color(0xFF1F2833)
        )
      }

      // Immersive 404 Cyberpunk Error Screen overlay
      AnimatedVisibility(
        visible = isError,
        enter = fadeIn(animationSpec = tween(500)),
        exit = fadeOut(animationSpec = tween(500)),
        modifier = Modifier.fillMaxSize()
      ) {
        TommiOsErrorPage(
          currentUrl = targetUrl,
          onRetry = {
            isError = false
            isLoading = true
            loadProgress = 0
            webViewInstance?.loadUrl(targetUrl)
          },
          onEditUrlClick = {
            tempUrlInput = targetUrl
            showUrlConfigDialog = true
          }
        )
      }
    }
  }

  // URL Config dialog for smart network troubleshooting
  if (showUrlConfigDialog) {
    AlertDialog(
      onDismissRequest = { showUrlConfigDialog = false },
      containerColor = Color(0xFF1F2833),
      titleContentColor = Color(0xFF66FCF1),
      textContentColor = Color(0xFFC5C6C7),
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = null,
            tint = Color(0xFF66FCF1),
            modifier = Modifier.size(24.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Configure Connection",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
          )
        }
      },
      text = {
        Column(modifier = Modifier.fillMaxWidth()) {
          Text(
            text = "Android devices can struggle resolving '.local' hostnames on some routers. If connection fails, try using your host machine's local IP address (e.g. http://192.168.1.100:3000).",
            fontSize = 12.sp,
            lineHeight = 16.sp,
            color = Color(0xFFC5C6C7),
            modifier = Modifier.padding(bottom = 16.dp)
          )
          
          OutlinedTextField(
            value = tempUrlInput,
            onValueChange = { tempUrlInput = it },
            label = { Text("Tommi OS Address", color = Color(0xFF66FCF1)) },
            placeholder = { Text("https://tommi-os.local:3000") },
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = Color(0xFF66FCF1),
              unfocusedBorderColor = Color(0xFF45A29E),
              focusedTextColor = Color.White,
              unfocusedTextColor = Color.White,
              focusedContainerColor = Color(0xFF0B0C10),
              unfocusedContainerColor = Color(0xFF0B0C10)
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
              keyboardType = KeyboardType.Uri,
              imeAction = ImeAction.Done
            ),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("url_input_field")
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (tempUrlInput.isNotBlank()) {
              targetUrl = tempUrlInput.trim()
              sharedPref.edit().putString("target_url", targetUrl).apply()
              showUrlConfigDialog = false
              
              // Trigger reload immediately
              isError = false
              isLoading = true
              loadProgress = 0
              webViewInstance?.loadUrl(targetUrl)
            }
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF66FCF1),
            contentColor = Color(0xFF0B0C10)
          ),
          modifier = Modifier.testTag("save_url_button")
        ) {
          Text("Save & Connect", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(
          onClick = { showUrlConfigDialog = false },
          colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFC5C6C7))
        ) {
          Text("Cancel")
        }
      }
    )
  }
}

@Composable
fun StatusIndicatorDot(isError: Boolean) {
  val infiniteTransition = rememberInfiniteTransition(label = "pulse")
  val alpha by infiniteTransition.animateFloat(
    initialValue = 0.3f,
    targetValue = 1.0f,
    animationSpec = infiniteRepeatable(
      animation = tween(1000, easing = EaseInOutQuad),
      repeatMode = RepeatMode.Reverse
    ),
    label = "dot_alpha"
  )

  val color = if (isError) Color(0xFFFF4D4D) else Color(0xFF00FFCC)

  Box(
    modifier = Modifier
      .size(10.dp)
      .graphicsLayer(alpha = alpha)
      .drawBehind {
        drawCircle(
          color = color,
          radius = size.minDimension / 2
        )
      }
  )
}

@Composable
fun TommiOsErrorPage(
  currentUrl: String,
  onRetry: () -> Unit,
  onEditUrlClick: () -> Unit
) {
  val scrollState = rememberScrollState()
  
  // Custom infinite floating/breathing motion path for the robot head
  val infiniteTransition = rememberInfiniteTransition(label = "floating_robot")
  
  val translateY by infiniteTransition.animateFloat(
    initialValue = -16f,
    targetValue = 16f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 2400, easing = EaseInOutSine),
      repeatMode = RepeatMode.Reverse
    ),
    label = "translation_y"
  )
  
  val scale by infiniteTransition.animateFloat(
    initialValue = 0.97f,
    targetValue = 1.03f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 2400, easing = EaseInOutSine),
      repeatMode = RepeatMode.Reverse
    ),
    label = "scale"
  )

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0xFF0B0C10))
      .verticalScroll(scrollState)
      .padding(horizontal = 24.dp, vertical = 16.dp)
      .testTag("error_page_container"),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    // 404 Header Tag
    Box(
      modifier = Modifier
        .clip(RoundedCornerShape(4.dp))
        .background(Color(0xFFFF3B30).copy(alpha = 0.15f))
        .border(1.dp, Color(0xFFFF3B30), RoundedCornerShape(4.dp))
        .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
      Text(
        text = "ERROR 404 - SYSTEM NOT FOUND",
        color = Color(0xFFFF3B30),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.5.sp,
        fontFamily = FontFamily.Monospace
      )
    }

    Spacer(modifier = Modifier.height(32.dp))

    // Robot Head Container with Cyberpunk Glow Base
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier
        .size(190.dp)
        .graphicsLayer {
          translationY = translateY
          scaleX = scale
          scaleY = scale
        }
    ) {
      // Ambient radial cyan halo background aura
      Box(
        modifier = Modifier
          .size(150.dp)
          .clip(CircleShape)
          .background(
            Brush.radialGradient(
              colors = listOf(
                Color(0xFF66FCF1).copy(alpha = 0.25f),
                Color.Transparent
              )
            )
          )
      )

      // Main Floating Graphic (custom generated robot head)
      Image(
        painter = painterResource(id = R.drawable.img_app_icon),
        contentDescription = "Floating robot diagnostic avatar",
        modifier = Modifier
          .size(130.dp)
          .clip(CircleShape)
          .border(2.dp, Color(0xFF66FCF1), CircleShape),
        contentScale = ContentScale.Crop
      )
    }

    Spacer(modifier = Modifier.height(24.dp))

    // Title
    Text(
      text = "Tommi OS Not Found",
      color = Color.White,
      fontSize = 24.sp,
      fontWeight = FontWeight.Bold,
      fontFamily = FontFamily.SansSerif,
      textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(8.dp))

    // Subtitle showing unreachable address
    Text(
      text = "Could not connect to $currentUrl",
      color = Color(0xFFC5C6C7).copy(alpha = 0.7f),
      fontSize = 13.sp,
      fontFamily = FontFamily.Monospace,
      textAlign = TextAlign.Center,
      modifier = Modifier.padding(horizontal = 16.dp)
    )

    Spacer(modifier = Modifier.height(28.dp))

    // Troubleshooting Instructions Card
    Card(
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2833)),
      elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
      modifier = Modifier
        .fillMaxWidth()
        .border(1.dp, Color(0xFF45A29E).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
    ) {
      Column(
        modifier = Modifier.padding(20.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(bottom = 12.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = Color(0xFF66FCF1),
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "TROUBLESHOOTING CHECKLIST",
            color = Color(0xFF66FCF1),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            fontFamily = FontFamily.Monospace
          )
        }

        Divider(color = Color(0xFF45A29E).copy(alpha = 0.2f), modifier = Modifier.padding(bottom = 16.dp))

        TroubleshootItem(
          number = "1",
          title = "Check the Router",
          desc = "Verify your phone is connected to the same Wi-Fi network as the Tommi OS server."
        )

        Spacer(modifier = Modifier.height(16.dp))

        TroubleshootItem(
          number = "2",
          title = "Check Power Connection",
          desc = "Ensure the Tommi OS host machine is fully booted, running, and plugged into power."
        )

        Spacer(modifier = Modifier.height(16.dp))

        TroubleshootItem(
          number = "3",
          title = "Verify Port & Firewall",
          desc = "Ensure that service port 3000 is open and not blocked by the host machine's firewall."
        )
      }
    }

    Spacer(modifier = Modifier.height(32.dp))

    // Interactive Action Row
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Re-configure URL Outlined button
      OutlinedButton(
        onClick = onEditUrlClick,
        modifier = Modifier
          .weight(1f)
          .height(50.dp)
          .testTag("edit_url_button"),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.outlinedButtonColors(
          contentColor = Color(0xFF66FCF1)
        ),
        border = BorderStroke(1.dp, Color(0xFF66FCF1))
      ) {
        Icon(
          imageVector = Icons.Default.Edit,
          contentDescription = null,
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("Change IP", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
      }

      // Reconnect/Retry Filled Button
      Button(
        onClick = onRetry,
        modifier = Modifier
          .weight(1.2f)
          .height(50.dp)
          .testTag("retry_button"),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = Color(0xFF66FCF1),
          contentColor = Color(0xFF0B0C10)
        )
      ) {
        Icon(
          imageVector = Icons.Default.Refresh,
          contentDescription = null,
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("Reconnect", fontWeight = FontWeight.Bold, fontSize = 14.sp)
      }
    }
  }
}

@Composable
fun TroubleshootItem(number: String, title: String, desc: String) {
  Row(
    verticalAlignment = Alignment.Top,
    modifier = Modifier.fillMaxWidth()
  ) {
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier
        .size(24.dp)
        .clip(CircleShape)
        .background(Color(0xFF66FCF1).copy(alpha = 0.12f))
        .border(1.dp, Color(0xFF66FCF1), CircleShape)
    ) {
      Text(
        text = number,
        color = Color(0xFF66FCF1),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace
      )
    }

    Spacer(modifier = Modifier.width(12.dp))

    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        color = Color.White,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = desc,
        color = Color(0xFFC5C6C7),
        fontSize = 12.sp,
        lineHeight = 16.sp
      )
    }
  }
}
