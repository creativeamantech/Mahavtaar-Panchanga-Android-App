package com.mahavtaar.panchanga.firebase

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mahavtaar.panchanga.ui.components.ChakraPetalAccentBar
import com.mahavtaar.panchanga.ui.components.LanguageTogglePill
import com.mahavtaar.panchanga.ui.components.MahavtaarBrandLogo
import com.mahavtaar.panchanga.ui.components.PanchangaCard
import com.mahavtaar.panchanga.ui.components.StatusPill
import com.mahavtaar.panchanga.ui.localization.AccessControlStrings
import com.mahavtaar.panchanga.ui.localization.LocalAppLanguage
import com.mahavtaar.panchanga.ui.localization.LocalExtendedStrings
import com.mahavtaar.panchanga.ui.theme.LocalVedicColors

@Composable
fun ResolvingAccessScreen(
    email: String,
    onToggleLanguage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = LocalAppLanguage.current
    val ac = AccessControlStrings.forLanguage(lang)

    GateScreenFrame(
        icon = Icons.Rounded.VerifiedUser,
        accentColor = MaterialTheme.colorScheme.primary,
        title = ac.resolvingAccessTitle,
        subtitle = ac.resolvingAccessSubtitle,
        emailPill = email.takeIf { it.isNotBlank() },
        onToggleLanguage = onToggleLanguage,
        testTag = "gate_resolving_screen",
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

@Composable
fun PendingApprovalScreen(
    email: String,
    uid: String,
    isChecking: Boolean,
    statusMessage: String?,
    onCheckStatus: () -> Unit,
    onSignOut: () -> Unit,
    onToggleLanguage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = LocalAppLanguage.current
    val ac = AccessControlStrings.forLanguage(lang)
    val ext = LocalExtendedStrings.current
    val vedic = LocalVedicColors.current

    GateScreenFrame(
        icon = Icons.Rounded.Schedule,
        accentColor = vedic.caution,
        title = ac.pendingApprovalTitle,
        subtitle = ac.pendingApprovalSubtitle,
        emailPill = email,
        onToggleLanguage = onToggleLanguage,
        testTag = "gate_pending_approval_screen",
        modifier = modifier
    ) {
        PanchangaCard {
            Text(
                text = ac.pendingApprovalHint,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "ID: $uid",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (!statusMessage.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = vedic.cautionContainer,
                border = BorderStroke(1.dp, vedic.caution.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = vedic.caution,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        Button(
            onClick = onCheckStatus,
            enabled = !isChecking,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 50.dp)
                .testTag("pending_check_status_button"),
            shape = MaterialTheme.shapes.large
        ) {
            if (isChecking) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(ac.checkingApprovalLabel)
            } else {
                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(ac.checkApprovalButton)
            }
        }

        OutlinedButton(
            onClick = onSignOut,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("pending_sign_out_button"),
            shape = MaterialTheme.shapes.large
        ) {
            Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(ext.signOutButton)
        }
    }
}

@Composable
fun SuspendedAccessScreen(
    email: String,
    onSignOut: () -> Unit,
    onToggleLanguage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = LocalAppLanguage.current
    val ac = AccessControlStrings.forLanguage(lang)
    val ext = LocalExtendedStrings.current
    val vedic = LocalVedicColors.current

    GateScreenFrame(
        icon = Icons.Rounded.Lock,
        accentColor = vedic.inauspicious,
        title = ac.suspendedTitle,
        subtitle = ac.suspendedSubtitle,
        emailPill = email,
        onToggleLanguage = onToggleLanguage,
        testTag = "gate_suspended_screen",
        modifier = modifier
    ) {
        OutlinedButton(
            onClick = onSignOut,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 50.dp)
                .testTag("suspended_sign_out_button"),
            shape = MaterialTheme.shapes.large
        ) {
            Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(ext.signOutButton)
        }
    }
}

@Composable
fun SessionDisplacedScreen(
    activeDeviceName: String,
    onSignInAgain: () -> Unit,
    onToggleLanguage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = LocalAppLanguage.current
    val ac = AccessControlStrings.forLanguage(lang)
    val vedic = LocalVedicColors.current

    GateScreenFrame(
        icon = Icons.Rounded.Devices,
        accentColor = vedic.inauspicious,
        title = ac.sessionDisplacedTitle,
        subtitle = ac.sessionDisplacedSubtitle,
        emailPill = "${ac.displacedByDevicePrefix}: $activeDeviceName",
        onToggleLanguage = onToggleLanguage,
        testTag = "gate_session_displaced_screen",
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = vedic.inauspiciousContainer,
            border = BorderStroke(1.dp, vedic.inauspicious.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.WarningAmber,
                    contentDescription = null,
                    tint = vedic.inauspicious,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "${ac.displacedByDevicePrefix}: $activeDeviceName",
                    style = MaterialTheme.typography.titleSmall,
                    color = vedic.inauspicious,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Button(
            onClick = onSignInAgain,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .testTag("displaced_sign_in_again_button"),
            shape = MaterialTheme.shapes.large
        ) {
            Icon(Icons.Rounded.VerifiedUser, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(ac.signInHereAgainButton)
        }
    }
}

@Composable
private fun GateScreenFrame(
    icon: ImageVector,
    accentColor: Color,
    title: String,
    subtitle: String,
    emailPill: String?,
    onToggleLanguage: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(20.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MahavtaarBrandLogo(size = 42.dp)
                LanguageTogglePill(onToggleLanguage = onToggleLanguage)
            }

            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.2.dp, accentColor.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    ChakraPetalAccentBar(height = 4.dp)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = title,
                                tint = accentColor,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        if (!emailPill.isNullOrBlank()) {
                            StatusPill(
                                label = emailPill,
                                textColor = accentColor,
                                containerColor = accentColor.copy(alpha = 0.14f)
                            )
                        }

                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        content()
                    }
                }
            }
        }
    }
}
