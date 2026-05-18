package com.example.hoehenmesser

import android.content.Context
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.activity.compose.LocalActivity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import com.example.hoehenmesser.ui.theme.Hoehenmesser
import kotlin.math.pow
import androidx.core.content.edit

class MainActivity : ComponentActivity() {

    private val viewModel by viewModels<HoehenViewModel>()
    private var sensorManager: SensorManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getPreferences(MODE_PRIVATE)
        viewModel.referenzDruck = prefs.getFloat("referenz_druck", 1013.25f)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager?
        val sensor = sensorManager?.getDefaultSensor(Sensor.TYPE_PRESSURE)
        sensorManager?.registerListener(sensorListener, sensor, SensorManager.SENSOR_DELAY_NORMAL)

        setContent {
            Hoehenmesser {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    HoehenScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    // bei schließung der app wird der sensor ausgeschalten
    override fun onDestroy() {
        super.onDestroy()
        sensorManager?.unregisterListener(sensorListener)
    }

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            val druck = event?.values?.get(0) ?: return
            viewModel.aktuellerDruck = druck
            // Höhenformel
            viewModel.berechneteHoehe = (288.15 / 0.0065) * (1.0 - (druck / viewModel.referenzDruck).toDouble().pow(1.0 / 5.255))
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }
}

class HoehenViewModel : ViewModel() {
    var aktuellerDruck  by mutableFloatStateOf(0f)
    var referenzDruck   by mutableFloatStateOf(1013.25f)   // Referenzdruck = 1013.25 hPa = Standard-Meereshöhe
    var berechneteHoehe by mutableDoubleStateOf(0.0)
}

@Composable
fun HoehenScreen(viewModel: HoehenViewModel, modifier: Modifier = Modifier) {
    var eingabe by remember { mutableStateOf("") }
    val prefs = (LocalActivity.current as MainActivity).getPreferences(Context.MODE_PRIVATE)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Höhenmesser",
            style = MaterialTheme.typography.headlineMedium
        )

        // Höhe
        Card(modifier = Modifier.fillMaxWidth(),elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)) {

            Column(modifier = Modifier.padding(16.dp)) {
                Text("Höhe", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "%.1f m".format(viewModel.berechneteHoehe),
                    style = MaterialTheme.typography.headlineLarge,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Luftdruck
        Card(modifier = Modifier.fillMaxWidth(),elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Luftdruck", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "%.2f hPa".format(viewModel.aktuellerDruck),
                    style = MaterialTheme.typography.bodyLarge,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Kalibrierung
        Card(modifier = Modifier.fillMaxWidth(),elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Kalibrierung", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Aktuelle Höhe eingeben (in Metern):",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = eingabe,
                        onValueChange = { eingabe = it },
                        label = { Text("Bekannte Höhe (m)") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { kalibrieren(eingabe, viewModel, prefs) }
                        ),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Button(onClick = { kalibrieren(eingabe, viewModel, prefs) }) {
                        Text("OK")
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Referenzdruck: %.2f hPa".format(viewModel.referenzDruck),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

// berechnet p0 aus höhe und druck
fun kalibrieren(eingabe: String, viewModel: HoehenViewModel, prefs: SharedPreferences) {
    val tatsaechlieHoehe = eingabe.toDoubleOrNull() ?: return
    // umgestellt: p = p0 * (1 - 0,0065·h/288,15)^5,255 -> p0 = p / (1 - 0.0065 * h/ 288.15 )^5.255
    val p0 = viewModel.aktuellerDruck / (1.0 - 0.0065 * tatsaechlieHoehe / 288.15).pow(5.255)
    viewModel.referenzDruck = p0.toFloat()
    prefs.edit { putFloat("referenz_druck", p0.toFloat()) }
}

@Preview(showBackground = true)
@Composable
fun HoehenScreenPreview() {
    Hoehenmesser {
        HoehenScreen(viewModel = HoehenViewModel())
    }
}