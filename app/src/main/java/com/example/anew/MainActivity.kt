package com.example.anew

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat.finishAffinity
import androidx.core.app.ServiceCompat.startForeground
import androidx.core.content.ContextCompat
import com.example.anew.ui.theme.NewTheme

fun isAccessibilityServiceEnabled(context: Context, service: Class<out android.accessibilityservice.AccessibilityService>): Boolean {
    val expectedComponentName = ComponentName(context, service)

    val enabledServicesSetting = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false

    val colonSplitter = TextUtils.SimpleStringSplitter(':')
    colonSplitter.setString(enabledServicesSetting)

    for (serviceName in colonSplitter) {
        val enabledComponent = ComponentName.unflattenFromString(serviceName)
        if (enabledComponent == expectedComponentName) {
            return true
        }
    }

    return false
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if(!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package: clickit"))
            startActivityForResult(intent, 100)
        }
        if (!isAccessibilityServiceEnabled(this, ClickService::class.java)) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }


        enableEdgeToEdge()
        setContent {
            Content()
        }
    }
}

@Composable
fun MyButton(modifier: Modifier) {
    val context = LocalContext.current

    Button(
        onClick = {
            val intent = Intent(context, FloatingBubbleService::class.java)
            context.startService(intent)
        },
        modifier = modifier,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text("Start")
    }
}


@Composable
fun Content() {
    NewTheme {
        Surface (
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.padding(WindowInsets.statusBars.asPaddingValues())){
                Row(modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .padding(start = 12.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("Clickit")
                }

                Column(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                ){
                    MyButton(
                        modifier = Modifier
                            .height(40.dp)
                            .fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Preview(
    showBackground = true
)
@Composable
fun Preview() {
    Content()
}