package com.example.myapplication

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.Directory
import com.drew.metadata.Metadata
import com.drew.metadata.Tag
import com.example.myapplication.ui.theme.MyApplicationTheme
import java.io.IOException

data class HistoryItem(
    val uri: Uri,
    val timestamp: Long,
    val imageName: String
)

// EXIF标签汉化映射
val exifTagTranslations = mapOf(
    // 相机信息
    "Make" to "相机品牌",
    "Model" to "相机型号",
    "Software" to "软件版本",
     "DateTime" to "拍摄时间",
    "DateTimeOriginal" to "原始日期时间",
    "DateTimeDigitized" to "数字化时间",
    "CreateDate" to "创建日期",
    
    // 曝光信息
    "ExposureTime" to "曝光时间",
    "FNumber" to "光圈值",
    "ExposureProgram" to "曝光程序",
    "ISO" to "ISO",
    "ExposureMode" to "曝光模式",
    "WhiteBalance" to "白平衡",
    "MeteringMode" to "测光模式",
    "Flash" to "闪光灯",
    "ExposureCompensation" to "曝光补偿",
    "MaxApertureValue" to "最大光圈值",
    "BrightnessValue" to "亮度值",
    "LightSource" to "光源",
    
    // 焦距信息
    "FocalLength" to "焦距",
    "FocalLengthIn35mmFilm" to "35mm等效焦距",
    
    // 图像信息
    "ImageWidth" to "图像宽度",
    "ImageHeight" to "图像高度",
    "Orientation" to "方向",
    "ResolutionUnit" to "分辨率单位",
    "XResolution" to "X分辨率",
    "YResolution" to "Y分辨率",
    "ExifImageWidth" to "Exif图像宽度",
    "ExifImageHeight" to "Exif图像高度",
    "ColorSpace" to "色彩空间",
    "FileSource" to "文件来源",
    "SceneType" to "场景类型",
    "SceneCaptureType" to "场景Capture类型",
    "ComponentsConfiguration" to "ComponentsConfiguration",
    "CompressedBitsPerPixel" to "压缩BitsPer像素",
    "CustomRendered" to "CustomRendered",
    "DigitalZoomRatio" to "数码变焦比",
    "ExifVersion" to "Exif版本",
    "FlashpixVersion" to "Flashpix版本",
    "LensInfo" to "LensInfo",
    "LensModel" to "LensModel",
    "RecommendedExposureIndex" to "RecommendedExposureIndex",
    "SensitivityType" to "SensitivityType",
    "UserComment" to "用户注释",
    "Contrast" to "对比度",
    "Saturation" to "饱和度",
    "Sharpness" to "锐度",
    
    // 快门信息
    "ShutterCount" to "快门次数",
    "ShutterSpeedValue" to "快门速度",
    
    // 光圈信息
    "ApertureValue" to "光圈值",
    
    // 其他常见标签
    "GPSLatitude" to "GPS纬度",
    "GPSLongitude" to "GPS经度",
    "GPSAltitude" to "GPS海拔",
    "GPSDateTime" to "GPS时间"
)

// MIME类型到文件扩展名的映射
val mimeToExtensionMap = mapOf(
    // 常见图像格式
    "image/jpeg" to "JPG",
    "image/png" to "PNG",
    "image/gif" to "GIF",
    "image/webp" to "WEBP",
    "image/bmp" to "BMP",
    "image/tiff" to "TIFF",
    
    // RAW格式
    "image/x-canon-cr2" to "CR2",
    "image/x-canon-cr3" to "CR3",
    "image/x-nikon-nef" to "NEF",
    "image/x-sony-arw" to "ARW",
    "image/x-adobe-dng" to "DNG",
    "image/x-panasonic-rw2" to "RW2",
    "image/x-olympus-orf" to "ORF",
    "image/x-pentax-pef" to "PEF",
    "image/x-samsung-srf" to "SRF",
    "image/x-foveon-x3f" to "X3F"
)

// 从URI和文件名中提取文件格式
fun extractFileExtension(uri: Uri, fileName: String, context: android.content.Context? = null): String {
    // 首先尝试从文件名中提取
    val extensionFromName = fileName.substringAfterLast('.', "").uppercase()
    if (extensionFromName.isNotEmpty()) {
        return extensionFromName
    }
    
    // 然后尝试从URI路径中提取
    val uriPath = uri.path ?: ""
    val extensionFromPath = uriPath.substringAfterLast('.', "").uppercase()
    if (extensionFromPath.isNotEmpty()) {
        return extensionFromPath
    }
    
    // 尝试从MIME类型获取
    if (context != null) {
        try {
            val mime = context.contentResolver.getType(uri)
            if (mime != null) {
                // 直接映射
                val extensionFromMime = mimeToExtensionMap[mime]
                if (extensionFromMime != null) {
                    return extensionFromMime
                }
                // 如果是image/*类型但不在映射中，返回通用格式
                if (mime.startsWith("image/")) {
                    return mime.substringAfter("image/").uppercase()
                }
            }
        } catch (e: Exception) {
            // 忽略异常
        }
    }
    
    // 常见的RAW格式扩展名
    val rawExtensions = listOf("CR2", "CR3", "NEF", "ARW", "DNG", "RW2", "ORF", "PEF", "SRF", "X3F")
    
    // 常见的图像格式扩展名
    val imageExtensions = listOf("JPG", "JPEG", "PNG", "GIF", "WEBP", "BMP", "TIFF")
    
    // 检查URI中是否包含常见的图像格式关键字
    val uriString = uri.toString().uppercase()
    for (ext in rawExtensions + imageExtensions) {
        if (uriString.contains(".${ext}") || uriString.contains("%2E${ext}")) {
            return ext
        }
    }
    
    // 如果都找不到，返回"未知格式"
    return "未知格式"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                EXIFToolApp()
            }
        }
    }
}

@Composable
fun EXIFToolApp() {
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var exifData by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    var historyList by remember { mutableStateOf<List<HistoryItem>>(emptyList()) }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data: Intent? = result.data
            val uri = data?.data
            if (uri != null) {
                selectedImageUri = uri
                errorMessage = null
                
                // 添加到历史记录
                val imageName = uri.lastPathSegment ?: "未知图片"
                val historyItem = HistoryItem(
                    uri = uri,
                    timestamp = System.currentTimeMillis(),
                    imageName = imageName
                )
                historyList = listOf(historyItem) + historyList
                
                // 在后台线程中解析EXIF数据，避免阻塞UI线程
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val metadata = ImageMetadataReader.readMetadata(context.contentResolver.openInputStream(uri)!!)
                        val exifMap = mutableMapOf<String, String>()
                        val tagPriority = mutableMapOf<String, Int>()
                        
                        // 定义目录优先级，包含索尼ARW格式特有的目录
                        val directoryPriority = mapOf(
                            "Exif" to 1,
                            "GPS" to 2,
                            "IFD0" to 3,
                            "IFD1" to 4,
                            "Sony" to 5,
                            "SonyMakernote" to 6,
                            "MakerNote" to 7,
                            "JFIF" to 8,
                            "PNG" to 9,
                            "WebP" to 10
                        )
                        
                        // 专门用于存储快门次数的候选值，存储元数据以便更好地筛选
                        data class ShutterCandidate(
                            val value: String,
                            val priority: Int,
                            val directoryName: String,
                            val tagName: String,
                            val isFromRawValue: Boolean
                        )
                        
                        val shutterCountCandidates = mutableListOf<ShutterCandidate>()
                        
                        // 首先处理所有标签
                        for (directory: Directory in metadata.directories) {
                            for (tag: Tag in directory.tags) {
                                val tagName = tag.tagName
                                val description = tag.description ?: ""
                                val tagNameLower = tagName.lowercase()
                                val currentPriority = directoryPriority[directory.name] ?: 10
                                
                                // 尝试直接从Directory获取原始数值，用于快门次数标签
                                var rawValue: String? = null
                                try {
                                    val tagType = tag.tagType
                                    if (directory.containsTag(tagType)) {
                                        when {
                                            directory.hasTagName(tagType) -> {
                                                val objectValue = directory.getObject(tagType)
                                                if (objectValue != null) {
                                                    when (objectValue) {
                                                        is Int -> rawValue = objectValue.toString()
                                                        is Long -> rawValue = objectValue.toString()
                                                        is Short -> rawValue = objectValue.toString()
                                                        is Number -> rawValue = objectValue.toString()
                                                        is String -> {
                                                            val numStr = objectValue.replace("[^0-9]", "")
                                                            if (numStr.isNotEmpty()) {
                                                                rawValue = numStr
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    // 获取原始值失败，忽略
                                }
                                
                                val dirNameLower = directory.name.lowercase()
                                
                                // 首先检查是否是明显不相关的标签，直接跳过
                                val isIrrelevantTag = 
                                    tagNameLower.contains("byte count") ||
                                    tagNameLower.contains("bytecount") ||
                                    tagNameLower.contains("strip") ||
                                    tagNameLower.contains("tile") ||
                                    tagNameLower.contains("thumbnail") ||
                                    tagNameLower.contains("jpeg") ||
                                    tagNameLower.contains("pixel") ||
                                    tagNameLower.contains("width") ||
                                    tagNameLower.contains("height") ||
                                    tagNameLower.contains("resolution") ||
                                    tagNameLower.contains("offset") ||
                                    tagNameLower.contains("size") && !tagNameLower.contains("shutter") && !tagNameLower.contains("release") && !tagNameLower.contains("count") ||
                                    tagNameLower.contains("length") && !tagNameLower.contains("shutter") && !tagNameLower.contains("release") && !tagNameLower.contains("count")
                                
                                if (!isIrrelevantTag) {
                                    // 更精确地检查是否是快门次数相关标签
                                    val isHighConfidenceShutterCountTag = 
                                        (tagNameLower == "shuttercount" || 
                                         tagNameLower == "shutter count" ||
                                         tagNameLower == "shutter-release count" ||
                                         tagNameLower == "releasecount" ||
                                         tagNameLower == "release count" ||
                                         tagNameLower == "快门次数" ||
                                         tagNameLower == "快门释放次数" ||
                                         (tagNameLower.contains("shutter") && tagNameLower.contains("count") && !tagNameLower.contains("mode")) ||
                                         (tagNameLower.contains("release") && tagNameLower.contains("count") && !tagNameLower.contains("mode")) ||
                                         (tagNameLower.contains("actuation") && tagNameLower.contains("count")))
                                    
                                    val isMediumConfidenceShutterCountTag =
                                        ((tagNameLower.contains("shutter") && !tagNameLower.contains("mode")) ||
                                        (tagNameLower.contains("release") && !tagNameLower.contains("mode")) ||
                                        tagNameLower.contains("actuation")) &&
                                        !isIrrelevantTag
                                    
                                    if (isHighConfidenceShutterCountTag || isMediumConfidenceShutterCountTag) {
                                        // 优先使用原始数值
                                        val valueToUse = if (rawValue != null && rawValue.matches(Regex("\\d+"))) {
                                            rawValue
                                        } else {
                                            // 从description中提取数字
                                            val numStr = description.replace("[^0-9]", "")
                                            if (numStr.isNotEmpty()) numStr else null
                                        }
                                        
                                        if (valueToUse != null && valueToUse.isNotEmpty()) {
                                            // 计算优先级 - 高置信度标签优先级更高
                                            var priority = when {
                                                isHighConfidenceShutterCountTag && (dirNameLower.contains("sony") || dirNameLower.contains("makernote") || dirNameLower == "maker note") -> 1
                                                isHighConfidenceShutterCountTag -> 2
                                                isMediumConfidenceShutterCountTag && (dirNameLower.contains("sony") || dirNameLower.contains("makernote") || dirNameLower == "maker note") -> 3
                                                isMediumConfidenceShutterCountTag -> 4
                                                else -> 5
                                            }
                                            
                                            shutterCountCandidates.add(
                                                ShutterCandidate(
                                                    value = valueToUse,
                                                    priority = priority,
                                                    directoryName = directory.name,
                                                    tagName = tagName,
                                                    isFromRawValue = rawValue != null
                                                )
                                            )
                                        }
                                    } else {
                                        // 处理非快门次数相关的标签
                                        val translatedTagName = exifTagTranslations[tagName] ?: tagName
                                        val tagKey = translatedTagName
                                        
                                        if (!tagPriority.containsKey(tagKey) || currentPriority < tagPriority[tagKey]!!) {
                                            exifMap[tagKey] = description
                                            tagPriority[tagKey] = currentPriority
                                        }
                                    }
                                } else {
                                    // 处理明显不相关的标签
                                    val translatedTagName = exifTagTranslations[tagName] ?: tagName
                                    val tagKey = translatedTagName
                                    
                                    if (!tagPriority.containsKey(tagKey) || currentPriority < tagPriority[tagKey]!!) {
                                        exifMap[tagKey] = description
                                        tagPriority[tagKey] = currentPriority
                                    }
                                }
                            }
                        }
                        
                        // 添加调试信息，显示所有候选值
                        if (shutterCountCandidates.isNotEmpty()) {
                            val debugInfo = shutterCountCandidates.take(10).mapIndexed { i, candidate ->
                                "#${i + 1}: ${candidate.value} (P${candidate.priority}, ${candidate.directoryName}/${candidate.tagName}, Raw:${candidate.isFromRawValue})"
                            }.joinToString(" | ")
                            if (debugInfo.isNotEmpty()) {
                                exifMap["快门候选(调试)"] = debugInfo
                            }
                        }
                        
                        // 处理快门次数标签
                        if (shutterCountCandidates.isNotEmpty()) {
                            // 筛选和排序候选值 - 更严格的范围
                            val filteredCandidates = shutterCountCandidates.filter { candidate ->
                                val intValue = candidate.value.toIntOrNull()
                                // 更严格的筛选，排除不合理的值
                                intValue != null && 
                                intValue in 500..500000 && // 更真实的快门次数范围
                                intValue != 65535 && 
                                intValue != 65534 &&
                                intValue != 255 &&
                                intValue != 256 &&
                                intValue != 4095 &&
                                intValue != 4096 &&
                                intValue != 32767 &&
                                intValue != 32768
                            }
                            
                            // 按优先级排序，优先使用原始值，然后更精确的范围
                            val sortedCandidates = filteredCandidates.sortedWith(
                                compareBy(
                                    { it.priority },
                                    { !it.isFromRawValue }, // 优先原始值
                                    { 
                                        // 更精确的范围偏好
                                        val intVal = it.value.toIntOrNull() ?: 0
                                        when {
                                            intVal in 1000..200000 -> 0 // 最可能的范围
                                            intVal in 200001..400000 -> 1
                                            intVal in 500..999 || intVal in 400001..500000 -> 2
                                            else -> 3
                                        }
                                    },
                                    { it.value.length } // 优先较短的数字（避免字节拼接错误）
                                )
                            )
                            
                            // 选择最优的快门次数值
                            var bestShutterCount: String? = null
                            if (sortedCandidates.isNotEmpty()) {
                                bestShutterCount = sortedCandidates[0].value
                            } else {
                                // 如果严格筛选没有结果，稍微放宽范围
                                val relaxedCandidates = shutterCountCandidates.filter { 
                                    val intValue = it.value.toIntOrNull()
                                    intValue != null && 
                                    intValue in 100..1000000 && 
                                    intValue != 65535 && intValue != 65534
                                }.sortedWith(
                                    compareBy(
                                        { it.priority },
                                        { !it.isFromRawValue },
                                        { 
                                            val intVal = it.value.toIntOrNull() ?: 0
                                            when {
                                                intVal in 1000..300000 -> 0
                                                else -> 1
                                            }
                                        }
                                    )
                                )
                                if (relaxedCandidates.isNotEmpty()) {
                                    bestShutterCount = relaxedCandidates[0].value
                                }
                            }
                            
                            if (bestShutterCount != null) {
                                exifMap["快门次数"] = bestShutterCount
                                tagPriority["快门次数"] = 0
                            }
                        }
                        
                        withContext(Dispatchers.Main) {
                            exifData = exifMap
                            errorMessage = null
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            errorMessage = "读取EXIF数据失败: ${e.message}"
                            exifData = emptyMap()
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Filled.Home, contentDescription = "首页") },
                    label = { Text("首页") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Filled.List, contentDescription = "历史记录") },
                    label = { Text("历史记录") }
                )
            }
        },
        content = {
            paddingValues ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (selectedTab) {
                    0 -> {
                        // 首页内容
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Top
                        ) {
                            Text(
                                text = "EXIF 工具",
                                fontSize = 24.sp,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                                    intent.type = "image/*"
                                    pickImageLauncher.launch(intent)
                                },
                                modifier = Modifier.padding(bottom = 16.dp)
                            ) {
                                Text(text = "选择图片")
                            }

                            if (errorMessage != null) {
                                Text(
                                    text = errorMessage!!,
                                    color = androidx.compose.ui.graphics.Color.Red,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )
                            }

                            selectedImageUri?.let {uri ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp),
                                    elevation = CardDefaults.cardElevation(6.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "已选择图片",
                                            fontSize = 14.sp,
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )
                                        ImagePreview(uri = uri, context = context)
                                    }
                                }
                            }

                            if (exifData.isNotEmpty()) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(2f),
                                    elevation = CardDefaults.cardElevation(8.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Text(
                                            text = "EXIF 数据",
                                            fontSize = 18.sp,
                                            modifier = Modifier.padding(bottom = 16.dp)
                                        )
                                        val scrollState = rememberScrollState()
                                        Column(
                                            modifier = Modifier
                                                .verticalScroll(scrollState)
                                                .fillMaxHeight()
                                        ) {
                                            val entries = exifData.entries.toList()
                                            entries.forEachIndexed { index, entry ->
                                                val isHighlighted = entry.key.contains("ISO", ignoreCase = true) ||
                                                    entry.key.contains("Shutter", ignoreCase = true) ||
                                                    entry.key.contains("Exposure", ignoreCase = true) ||
                                                    entry.key.contains("Aperture", ignoreCase = true) ||
                                                    entry.key.contains("FNumber", ignoreCase = true) ||
                                                    entry.key.contains("光圈", ignoreCase = true) ||
                                                    entry.key.contains("快门", ignoreCase = true) ||
                                                    entry.key.contains("曝光", ignoreCase = true)
                                                
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = entry.key,
                                                        modifier = Modifier.weight(1f),
                                                        fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isHighlighted) androidx.compose.ui.graphics.Color.White else androidx.compose.ui.graphics.Color.Unspecified
                                                    )
                                                    Text(
                                                        text = entry.value,
                                                        modifier = Modifier.weight(1f),
                                                        textAlign = androidx.compose.ui.text.style.TextAlign.End,
                                                        fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isHighlighted) androidx.compose.ui.graphics.Color.White else androidx.compose.ui.graphics.Color.Unspecified
                                                    )
                                                }
                                                if (index < entries.size - 1) {
                                                    Divider(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(vertical = 4.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        // 历史记录内容
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Top
                        ) {
                            Text(
                                text = "历史记录",
                                fontSize = 24.sp,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            
                            if (historyList.isEmpty()) {
                                Text(
                                    text = "暂无历史记录",
                                    modifier = Modifier.padding(16.dp)
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(historyList) {
                                        historyItem ->
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 8.dp),
                                            elevation = CardDefaults.cardElevation(4.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(12.dp)
                                            ) {
                                                Text(
                                                    text = historyItem.imageName,
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                // 提取并显示照片格式
                                                val fileExtension = extractFileExtension(historyItem.uri, historyItem.imageName, context)
                                                Text(
                                                    text = "格式: $fileExtension",
                                                    fontSize = 12.sp,
                                                    color = androidx.compose.ui.graphics.Color.Gray
                                                )
                                                Text(
                                                    text = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date(historyItem.timestamp)),
                                                    fontSize = 12.sp,
                                                    color = androidx.compose.ui.graphics.Color.Gray
                                                )
                                                Button(
                                                    onClick = {
                                                        selectedImageUri = historyItem.uri
                                                        errorMessage = null
                                                        
                                                        // 重新解析EXIF数据
                                                        CoroutineScope(Dispatchers.IO).launch {
                                                            try {
                                                                val metadata = ImageMetadataReader.readMetadata(context.contentResolver.openInputStream(historyItem.uri)!!)
                                                                val exifMap = mutableMapOf<String, String>()
                                                                val tagPriority = mutableMapOf<String, Int>()
                                                                 
                                                                // 定义目录优先级，包含索尼ARW格式特有的目录
                                                                val directoryPriority = mapOf(
                                                                    "Exif" to 1,
                                                                    "GPS" to 2,
                                                                    "IFD0" to 3,
                                                                    "IFD1" to 4,
                                                                    "Sony" to 5,
                                                                    "SonyMakernote" to 6,
                                                                    "MakerNote" to 7,
                                                                    "JFIF" to 8,
                                                                    "PNG" to 9,
                                                                    "WebP" to 10
                                                                )
                                                                 
                                                                // 专门用于存储快门次数的候选值，存储元数据以便更好地筛选
                                                                data class ShutterCandidate(
                                                                    val value: String,
                                                                    val priority: Int,
                                                                    val directoryName: String,
                                                                    val tagName: String,
                                                                    val isFromRawValue: Boolean
                                                                )
                                                                
                                                                val shutterCountCandidates = mutableListOf<ShutterCandidate>()
                                                                 
                                                                // 首先处理所有标签
                                                                for (directory: Directory in metadata.directories) {
                                                                    for (tag: Tag in directory.tags) {
                                                                        val tagName = tag.tagName
                                                                        val description = tag.description ?: ""
                                                                        val tagNameLower = tagName.lowercase()
                                                                        val currentPriority = directoryPriority[directory.name] ?: 10
                                                                        
                                                                        // 尝试直接从Directory获取原始数值，用于快门次数标签
                                                                        var rawValue: String? = null
                                                                        try {
                                                                            val tagType = tag.tagType
                                                                            if (directory.containsTag(tagType)) {
                                                                                when {
                                                                                    directory.hasTagName(tagType) -> {
                                                                                        val objectValue = directory.getObject(tagType)
                                                                                        if (objectValue != null) {
                                                                                            when (objectValue) {
                                                                                                is Int -> rawValue = objectValue.toString()
                                                                                                is Long -> rawValue = objectValue.toString()
                                                                                                is Short -> rawValue = objectValue.toString()
                                                                                                is Number -> rawValue = objectValue.toString()
                                                                                                is String -> {
                                                                                                    val numStr = objectValue.replace("[^0-9]", "")
                                                                                                    if (numStr.isNotEmpty()) {
                                                                                                        rawValue = numStr
                                                                                                    }
                                                                                                }
                                                                                            }
                                                                                        }
                                                                                    }
                                                                                }
                                                                            }
                                                                        } catch (e: Exception) {
                                                                            // 获取原始值失败，忽略
                                                                        }
                                                                        
                                                                        val dirNameLower = directory.name.lowercase()
                                                                        
                                                                        // 更精确地检查是否是快门次数相关标签
                                                                        val isHighConfidenceShutterCountTag = 
                                                                            (tagNameLower == "shuttercount" || 
                                                                             tagNameLower == "shutter count" ||
                                                                             tagNameLower == "shutter-release count" ||
                                                                             tagNameLower == "releasecount" ||
                                                                             tagNameLower == "release count" ||
                                                                             tagNameLower == "快门次数" ||
                                                                             tagNameLower == "快门释放次数" ||
                                                                             (tagNameLower.contains("shutter") && tagNameLower.contains("count") && !tagNameLower.contains("mode")) ||
                                                                             (tagNameLower.contains("release") && tagNameLower.contains("count") && !tagNameLower.contains("mode")) ||
                                                                             (tagNameLower.contains("actuation") && tagNameLower.contains("count")))
                                                                        
                                                                        val isMediumConfidenceShutterCountTag =
                                                                            (tagNameLower.contains("shutter") && !tagNameLower.contains("mode")) ||
                                                                            (tagNameLower.contains("release") && !tagNameLower.contains("mode")) ||
                                                                            (tagNameLower.contains("actuation")) ||
                                                                            tagNameLower.contains("count")
                                                                        
                                                                        if (isHighConfidenceShutterCountTag || isMediumConfidenceShutterCountTag) {
                                                                            // 优先使用原始数值
                                                                            val valueToUse = if (rawValue != null && rawValue.matches(Regex("\\d+"))) {
                                                                                rawValue
                                                                            } else {
                                                                                // 从description中提取数字
                                                                                val numStr = description.replace("[^0-9]", "")
                                                                                if (numStr.isNotEmpty()) numStr else null
                                                                            }
                                                                            
                                                                            if (valueToUse != null && valueToUse.isNotEmpty()) {
                                                                                // 计算优先级 - 高置信度标签优先级更高
                                                                                var priority = when {
                                                                                    isHighConfidenceShutterCountTag && (dirNameLower.contains("sony") || dirNameLower.contains("makernote") || dirNameLower == "maker note") -> 1
                                                                                    isHighConfidenceShutterCountTag -> 2
                                                                                    isMediumConfidenceShutterCountTag && (dirNameLower.contains("sony") || dirNameLower.contains("makernote") || dirNameLower == "maker note") -> 3
                                                                                    isMediumConfidenceShutterCountTag -> 4
                                                                                    else -> 5
                                                                                }
                                                                                
                                                                                shutterCountCandidates.add(
                                                                                    ShutterCandidate(
                                                                                        value = valueToUse,
                                                                                        priority = priority,
                                                                                        directoryName = directory.name,
                                                                                        tagName = tagName,
                                                                                        isFromRawValue = rawValue != null
                                                                                    )
                                                                                )
                                                                            }
                                                                        } else {
                                                                            // 处理非快门次数相关的标签
                                                                            val translatedTagName = exifTagTranslations[tagName] ?: tagName
                                                                            val tagKey = translatedTagName
                                                                            
                                                                            if (!tagPriority.containsKey(tagKey) || currentPriority < tagPriority[tagKey]!!) {
                                                                                exifMap[tagKey] = description
                                                                                tagPriority[tagKey] = currentPriority
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                                
                                                                // 添加调试信息，显示所有候选值
                                                                if (shutterCountCandidates.isNotEmpty()) {
                                                                    val debugInfo = shutterCountCandidates.take(10).mapIndexed { i, candidate ->
                                                                        "#${i + 1}: ${candidate.value} (P${candidate.priority}, ${candidate.directoryName}/${candidate.tagName}, Raw:${candidate.isFromRawValue})"
                                                                    }.joinToString(" | ")
                                                                    if (debugInfo.isNotEmpty()) {
                                                                        exifMap["快门候选(调试)"] = debugInfo
                                                                    }
                                                                }
                                                                
                                                                // 处理快门次数标签
                                                                if (shutterCountCandidates.isNotEmpty()) {
                                                                    // 筛选和排序候选值 - 更严格的范围
                                                                    val filteredCandidates = shutterCountCandidates.filter { candidate ->
                                                                        val intValue = candidate.value.toIntOrNull()
                                                                        // 更严格的筛选，排除不合理的值
                                                                        intValue != null && 
                                                                        intValue in 500..500000 && // 更真实的快门次数范围
                                                                        intValue != 65535 && 
                                                                        intValue != 65534 &&
                                                                        intValue != 255 &&
                                                                        intValue != 256 &&
                                                                        intValue != 4095 &&
                                                                        intValue != 4096 &&
                                                                        intValue != 32767 &&
                                                                        intValue != 32768
                                                                    }
                                                                    
                                                                    // 按优先级排序，优先使用原始值，然后更精确的范围
                                                                    val sortedCandidates = filteredCandidates.sortedWith(
                                                                        compareBy(
                                                                            { it.priority },
                                                                            { !it.isFromRawValue }, // 优先原始值
                                                                            { 
                                                                                // 更精确的范围偏好
                                                                                val intVal = it.value.toIntOrNull() ?: 0
                                                                                when {
                                                                                    intVal in 1000..200000 -> 0 // 最可能的范围
                                                                                    intVal in 200001..400000 -> 1
                                                                                    intVal in 500..999 || intVal in 400001..500000 -> 2
                                                                                    else -> 3
                                                                                }
                                                                            },
                                                                            { it.value.length } // 优先较短的数字（避免字节拼接错误）
                                                                        )
                                                                    )
                                                                    
                                                                    // 选择最优的快门次数值
                                                                    var bestShutterCount: String? = null
                                                                    if (sortedCandidates.isNotEmpty()) {
                                                                        bestShutterCount = sortedCandidates[0].value
                                                                    } else {
                                                                        // 如果严格筛选没有结果，稍微放宽范围
                                                                        val relaxedCandidates = shutterCountCandidates.filter { 
                                                                            val intValue = it.value.toIntOrNull()
                                                                            intValue != null && 
                                                                            intValue in 100..1000000 && 
                                                                            intValue != 65535 && intValue != 65534
                                                                        }.sortedWith(
                                                                            compareBy(
                                                                                { it.priority },
                                                                                { !it.isFromRawValue },
                                                                                { 
                                                                                    val intVal = it.value.toIntOrNull() ?: 0
                                                                                    when {
                                                                                        intVal in 1000..300000 -> 0
                                                                                        else -> 1
                                                                                    }
                                                                                }
                                                                            )
                                                                        )
                                                                        if (relaxedCandidates.isNotEmpty()) {
                                                                            bestShutterCount = relaxedCandidates[0].value
                                                                        }
                                                                    }
                                                                    
                                                                    if (bestShutterCount != null) {
                                                                        exifMap["快门次数"] = bestShutterCount
                                                                        tagPriority["快门次数"] = 0
                                                                    }
                                                                }
                                                                 
                                                                withContext(Dispatchers.Main) {
                                                                    exifData = exifMap
                                                                    errorMessage = null
                                                                    selectedTab = 0 // 切换回首页
                                                                }
                                                            } catch (e: Exception) {
                                                                withContext(Dispatchers.Main) {
                                                                    errorMessage = "读取EXIF数据失败: ${e.message}"
                                                                    exifData = emptyMap()
                                                                }
                                                            }
                                                        }
                                                    },
                                                    modifier = Modifier
                                                        .padding(top = 8.dp)
                                                        .fillMaxWidth()
                                                        .height(36.dp)
                                                ) {
                                                    Text(text = "查看详情")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun ImagePreview(uri: Uri, context: android.content.Context) {
    var bitmapState by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var errorState by remember { mutableStateOf<String?>(null) }

    // 在LaunchedEffect中加载图片，避免在Composable函数体中使用try-catch
    LaunchedEffect(uri) {
        try {
            val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            bitmapState = bitmap
            errorState = null
        } catch (e: IOException) {
            errorState = "加载图片失败"
            bitmapState = null
        }
    }

    when {
        bitmapState != null -> {
            Image(
                                bitmap = bitmapState!!.asImageBitmap(),
                                contentDescription = "已选择图片",
                                modifier = Modifier
                                    .size(100.dp)
                                    .padding(bottom = 8.dp)
                            )
        }
        errorState != null -> {
            Text(text = errorState!!)
        }
        else -> {
            Text(text = "正在加载图片...")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EXIFToolAppPreview() {
    MyApplicationTheme {
        EXIFToolApp()
    }
}