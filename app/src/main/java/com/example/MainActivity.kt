package com.example

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
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
  
  var webViewInitError by remember { mutableStateOf<String?>(null) }
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
      if (webViewInitError != null) {
        TommiOsSystemCrashPage(
          errorMessage = webViewInitError ?: "Failed to initialize WebView",
          onRetry = {
            webViewInitError = null
            isError = false
            isLoading = true
            loadProgress = 0
          }
        )
      } else {
        // WebView Core Layer with robust try-catch framework for Android 16
        AndroidView(
          factory = { ctx ->
            try {
              // Primary Workaround: initialize using the applicationContext
              // (Bypasses resource redirect issues with Activity-scoped configurations in some Android 16 environments)
              WebView(ctx.applicationContext).apply {
                // Force software rendering mode to prevent cloud Mesa rendernode driver warnings
                setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
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
                    isError = hasError
                  }

                  override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                  ) {
                    super.onReceivedError(view, request, error)
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
                    handler?.proceed()
                  }
                }

                webChromeClient = object : WebChromeClient() {
                  override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)
                    loadProgress = newProgress
                  }

                  override fun onPermissionRequest(request: PermissionRequest?) {
                    request?.grant(request.resources)
                  }

                  override fun onGeolocationPermissionsShowPrompt(
                    origin: String?,
                    callback: GeolocationPermissions.Callback?
                  ) {
                    callback?.invoke(origin, true, false)
                  }
                }
                
                webViewInstance = this
                loadUrl(targetUrl)
              }
            } catch (e: Throwable) {
              try {
                // Secondary fallback attempt: initialize with standard Activity context
                WebView(ctx).apply {
                  // Force software rendering mode to prevent cloud Mesa rendernode driver warnings
                  setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
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
                      isError = hasError
                    }

                    override fun onReceivedError(
                      view: WebView?,
                      request: WebResourceRequest?,
                      error: WebResourceError?
                    ) {
                      super.onReceivedError(view, request, error)
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
                      handler?.proceed()
                    }
                  }

                  webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                      super.onProgressChanged(view, newProgress)
                      loadProgress = newProgress
                    }

                    override fun onPermissionRequest(request: PermissionRequest?) {
                      request?.grant(request.resources)
                    }

                    override fun onGeolocationPermissionsShowPrompt(
                      origin: String?,
                      callback: GeolocationPermissions.Callback?
                    ) {
                      callback?.invoke(origin, true, false)
                    }
                  }
                  
                  webViewInstance = this
                  loadUrl(targetUrl)
                }
              } catch (e2: Throwable) {
                // Catch all and save trace to show a fully responsive recovery layout
                webViewInitError = e2.localizedMessage ?: e2.toString()
                android.view.View(ctx)
              }
            }
          },
          update = { webView ->
            if (webViewInitError == null && webView is WebView) {
              if (webView.url != targetUrl && !isError && !isLoading) {
                webView.loadUrl(targetUrl)
              }
            }
          },
          modifier = Modifier
            .fillMaxSize()
            .testTag("webview_container")
        )

        // Immersive Boot & AI Initialization Screen Overlay
        AnimatedVisibility(
          visible = isLoading && !isError,
          enter = fadeIn(animationSpec = tween(400)),
          exit = fadeOut(animationSpec = tween(700)),
          modifier = Modifier.fillMaxSize()
        ) {
          TommiOsLoadingScreen()
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
        painter = rememberSafeImagePainter(id = R.drawable.img_app_icon, fallbackIcon = Icons.Default.WifiOff),
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

@Composable
fun TommiOsSystemCrashPage(
  errorMessage: String,
  onRetry: () -> Unit
) {
  val scrollState = rememberScrollState()
  
  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0xFF0B0C10))
      .verticalScroll(scrollState)
      .padding(horizontal = 24.dp, vertical = 32.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Box(
      modifier = Modifier
        .clip(RoundedCornerShape(4.dp))
        .background(Color(0xFFFF9500).copy(alpha = 0.15f))
        .border(1.dp, Color(0xFFFF9500), RoundedCornerShape(4.dp))
        .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
      Text(
        text = "SYSTEM COMPATIBILITY ALERTER",
        color = Color(0xFFFF9500),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.5.sp,
        fontFamily = FontFamily.Monospace
      )
    }

    Spacer(modifier = Modifier.height(24.dp))

    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier.size(140.dp)
    ) {
      Box(
        modifier = Modifier
          .size(110.dp)
          .clip(CircleShape)
          .background(
            Brush.radialGradient(
              colors = listOf(
                Color(0xFFFF9500).copy(alpha = 0.2f),
                Color.Transparent
              )
            )
          )
      )

      Image(
        painter = rememberSafeImagePainter(id = R.drawable.img_app_icon, fallbackIcon = Icons.Default.Warning),
        contentDescription = "Robot warning avatar",
        modifier = Modifier
          .size(100.dp)
          .clip(CircleShape)
          .border(2.dp, Color(0xFFFF9500), CircleShape),
        contentScale = ContentScale.Crop
      )
    }

    Spacer(modifier = Modifier.height(24.dp))

    Text(
      text = "Android 16 WebView Issue",
      color = Color.White,
      fontSize = 20.sp,
      fontWeight = FontWeight.Bold,
      textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
      text = "The device's built-in Android System WebView component failed to initialize securely.",
      color = Color(0xFFC5C6C7),
      fontSize = 13.sp,
      textAlign = TextAlign.Center,
      modifier = Modifier.padding(horizontal = 16.dp)
    )

    Spacer(modifier = Modifier.height(16.dp))

    Card(
      shape = RoundedCornerShape(8.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2833)),
      modifier = Modifier.fillMaxWidth()
    ) {
      Text(
        text = "Details: $errorMessage",
        color = Color(0xFFFF9500),
        fontSize = 12.sp,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.padding(12.dp)
      )
    }

    Spacer(modifier = Modifier.height(24.dp))

    Card(
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2833)),
      modifier = Modifier
        .fillMaxWidth()
        .border(1.dp, Color(0xFFFF9500).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
    ) {
      Column(modifier = Modifier.padding(20.dp)) {
        Text(
          text = "HOW TO SOLVE THIS",
          color = Color(0xFFFF9500),
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
          modifier = Modifier.padding(bottom = 12.dp)
        )

        TroubleshootItem(
          number = "1",
          title = "Update System WebView",
          desc = "Go to Google Play Store, search for 'Android System WebView', and ensure it is updated to the latest stable release."
        )

        Spacer(modifier = Modifier.height(12.dp))

        TroubleshootItem(
          number = "2",
          title = "Check Play Store Chrome",
          desc = "Sometimes updating Google Chrome on Android 16 also updates the shared WebView renderer engine."
        )

        Spacer(modifier = Modifier.height(12.dp))

        TroubleshootItem(
          number = "3",
          title = "Device System Update",
          desc = "Verify if a new Android 16 system software or security update is available to fix framework resource redirect bugs."
        )
      }
    }

    Spacer(modifier = Modifier.height(24.dp))

    Button(
      onClick = onRetry,
      modifier = Modifier
        .fillMaxWidth()
        .height(50.dp),
      shape = RoundedCornerShape(8.dp),
      colors = ButtonDefaults.buttonColors(
        containerColor = Color(0xFFFF9500),
        contentColor = Color(0xFF0B0C10)
      )
    ) {
      Icon(
        imageVector = Icons.Default.Refresh,
        contentDescription = null,
        modifier = Modifier.size(18.dp)
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text("Retry Initialization", fontWeight = FontWeight.Bold)
    }
  }
}

@Composable
fun rememberSafeImagePainter(id: Int, fallbackIcon: androidx.compose.ui.graphics.vector.ImageVector): androidx.compose.ui.graphics.painter.Painter {
  val context = LocalContext.current
  return remember(id, fallbackIcon) {
    try {
      val bitmap = BitmapFactory.decodeResource(context.resources, id)
      if (bitmap != null) {
        BitmapPainter(bitmap.asImageBitmap())
      } else {
        null
      }
    } catch (e: Throwable) {
      null
    }
  } ?: rememberVectorPainter(image = fallbackIcon)
}

@Composable
fun TommiOsLoadingScreen() {
  val tickerMessages = listOf(
    "TOMMI IS INITIALIZING...",
    "> POWERING UP COGNITIVE CORE",
    "> LOADING LOCAL INTELLIGENCE",
    "> INITIALIZING MEMORY",
    "> CONNECTING VISION SYSTEM",
    "> CALIBRATING VOICE ENGINE",
    "> CHECKING SYSTEM STATUS",
    "> PREPARING TOMMI",
    "> COGNITIVE SYSTEM ONLINE"
  )

  var currentMessageIndex by remember { mutableStateOf(0) }
  LaunchedEffect(Unit) {
    while (true) {
      kotlinx.coroutines.delay(2200)
      currentMessageIndex = (currentMessageIndex + 1) % tickerMessages.size
    }
  }

  val infiniteTransition = rememberInfiniteTransition(label = "tommi_loader")

  // Rotation of the orbital ring
  val rotationAngle by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 6500, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "rotation"
  )

  // Rotation of the outer secondary ring (opposite direction)
  val reverseRotationAngle by infiniteTransition.animateFloat(
    initialValue = 360f,
    targetValue = 0f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 8500, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "reverse_rotation"
  )

  // Pulsing core scale
  val corePulse by infiniteTransition.animateFloat(
    initialValue = 0.88f,
    targetValue = 1.12f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 2000, easing = EaseInOutSine),
      repeatMode = RepeatMode.Reverse
    ),
    label = "core_pulse"
  )

  // Glow core alpha pulse
  val coreAlpha by infiniteTransition.animateFloat(
    initialValue = 0.12f,
    targetValue = 0.38f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 2000, easing = EaseInOutSine),
      repeatMode = RepeatMode.Reverse
    ),
    label = "core_alpha"
  )

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(
        Brush.radialGradient(
          colors = listOf(
            Color(0xFF1F2833).copy(alpha = 0.5f),
            Color(0xFF0B0C10)
          ),
          radius = 1100f
        )
      )
      .testTag("tommi_loading_screen"),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      // 1. Central Orbital Thinking Component
      Box(
        modifier = Modifier.size(240.dp),
        contentAlignment = Alignment.Center
      ) {
        // Glowing Pulse Core
        Box(
          modifier = Modifier
            .size(150.dp)
            .graphicsLayer(
              scaleX = corePulse,
              scaleY = corePulse,
              alpha = coreAlpha
            )
            .background(
              Brush.radialGradient(
                colors = listOf(
                  Color(0xFF66FCF1),
                  Color.Transparent
                )
              ),
              shape = CircleShape
            )
        )

        // Custom Inner Orbital Ring (Rotating dashed circle)
        Canvas(
          modifier = Modifier
            .size(190.dp)
            .graphicsLayer(rotationZ = rotationAngle)
        ) {
          val strokeWidth = 1.5.dp.toPx()
          drawCircle(
            color = Color(0xFF66FCF1).copy(alpha = 0.6f),
            radius = size.minDimension / 2f,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
              width = strokeWidth,
              pathEffect = PathEffect.dashPathEffect(floatArrayOf(35f, 45f), 0f)
            )
          )
        }

        // Secondary Outer Ring (Counter-rotating)
        Canvas(
          modifier = Modifier
            .size(215.dp)
            .graphicsLayer(rotationZ = reverseRotationAngle)
        ) {
          val strokeWidth = 1.dp.toPx()
          drawCircle(
            color = Color(0xFF45A29E).copy(alpha = 0.4f),
            radius = size.minDimension / 2f,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
              width = strokeWidth,
              pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 65f), 0f)
            )
          )
        }

        // Central Logo/Text
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          Image(
            painter = rememberSafeImagePainter(id = R.drawable.img_app_icon, fallbackIcon = Icons.Default.Warning),
            contentDescription = "TOMMI OS Core",
            modifier = Modifier
              .size(64.dp)
              .graphicsLayer(
                scaleX = 1f + (corePulse - 1f) * 0.12f,
                scaleY = 1f + (corePulse - 1f) * 0.12f
              )
          )

          Spacer(modifier = Modifier.height(6.dp))

          Text(
            text = "TOMMI",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 5.sp,
            fontFamily = FontFamily.Monospace
          )

          Text(
            text = "INTELLIGENT OS",
            color = Color(0xFF45A29E),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
          )
        }
      }

      Spacer(modifier = Modifier.height(48.dp))

      // 2. Terminal Readout Ticker
      Box(
        modifier = Modifier
          .height(55.dp)
          .fillMaxWidth(),
        contentAlignment = Alignment.Center
      ) {
        Crossfade(
          targetState = tickerMessages[currentMessageIndex],
          animationSpec = tween(500, easing = EaseInOutSine),
          label = "ticker_crossfade"
        ) { message ->
          Text(
            text = message,
            color = if (message.startsWith(">")) Color(0xFF66FCF1) else Color.White,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // 3. Futuristic Scanning Micro-Bar
      Box(
        modifier = Modifier
          .width(160.dp)
          .height(2.dp)
          .background(Color(0xFF1F2833))
      ) {
        val scanX by infiniteTransition.animateFloat(
          initialValue = 0f,
          targetValue = 1f,
          animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
          ),
          label = "scan_bar_anim"
        )
        Box(
          modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(0.3f)
            .align(Alignment.CenterStart)
            .graphicsLayer {
              translationX = scanX * (160.dp.toPx() * 0.7f)
            }
            .background(
              Brush.horizontalGradient(
                colors = listOf(
                  Color.Transparent,
                  Color(0xFF66FCF1),
                  Color.Transparent
                )
              )
            )
        )
      }
    }
  }
}


