package net.jnlyrics.media;

import net.fabricmc.loader.api.FabricLoader;
import net.jnlyrics.JNLyricsMod;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

public class WindowsMediaProvider {
    private final AtomicReference<TrackInfo> currentTrack = new AtomicReference<>(new TrackInfo("NONE", "", "", "", 0, 0));
    private Process gsmtcProcess;
    private Thread gsmtcThread;
    private volatile boolean running = false;
    private String lastLoggedTrack = "";

    private static final String GSMTC_SCRIPT =
            "[Console]::OutputEncoding = [System.Text.Encoding]::UTF8\n" +
            "$myPid = $PID\n" +
            "Get-CimInstance Win32_Process | Where-Object { $_.CommandLine -like '* -File *gsmtc_bridge.ps1*' -and $_.ProcessId -ne $myPid } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }\n" +
            "Add-Type -AssemblyName System.Runtime.WindowsRuntime\n" +
            "$asTaskGeneric = [System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object { $_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1' }[0]\n" +
            "function Await-WinRT($winRtAsync, $returnType) {\n" +
            "    if (-not $winRtAsync) { return $null }\n" +
            "    try {\n" +
            "        $asTask = $asTaskGeneric.MakeGenericMethod($returnType)\n" +
            "        $task = $asTask.Invoke($null, @($winRtAsync))\n" +
            "        if ($task.Wait(1200)) { return $task.Result }\n" +
            "    } catch {}\n" +
            "    return $null\n" +
            "}\n" +
            "[Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager, Windows.Media, ContentType = WindowsRuntime] | Out-Null\n" +
            "$manager = Await-WinRT ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]::RequestAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager])\n" +
            "if (-not $manager) { exit 1 }\n" +
            "$cachedTitle = ''\n" +
            "$cachedArtist = ''\n" +
            "$cachedAlbum = ''\n" +
            "$cachedSourceApp = ''\n" +
            "$lastMediaCheck = 0\n" +
            "$lastDur = 0\n" +
            "$lastTrackKey = ''\n" +
            "$lastStatus = 'Unknown'\n" +
            "$lastSentTime = 0\n" +
            "while ($true) {\n" +
            "    try {\n" +
            "        $now = [DateTimeOffset]::UtcNow\n" +
            "        $nowMs = $now.ToUnixTimeMilliseconds()\n" +
            "        $curr = $manager.GetCurrentSession()\n" +
            "        if (-not $curr) {\n" +
            "            foreach ($s in $manager.GetSessions()) {\n" +
            "                try {\n" +
            "                    $p = $s.GetPlaybackInfo()\n" +
            "                    if ($p -and $p.PlaybackStatus -eq [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionPlaybackStatus]::Playing) {\n" +
            "                        $curr = $s\n" +
            "                        break\n" +
            "                    }\n" +
            "                } catch {}\n" +
            "            }\n" +
            "        }\n" +
            "        if ($curr) {\n" +
            "            $pInfo = $curr.GetPlaybackInfo()\n" +
            "            $status = if ($pInfo) { $pInfo.PlaybackStatus.ToString() } else { 'Unknown' }\n" +
            "            $tl = $curr.GetTimelineProperties()\n" +
            "            $rawPos = if ($tl -and $tl.Position) { [long]$tl.Position.TotalMilliseconds } else { 0 }\n" +
            "            $dur = if ($tl -and $tl.EndTime) { [long]$tl.EndTime.TotalMilliseconds } else { 0 }\n" +
            "            $lastUpd = if ($tl -and $tl.LastUpdatedTime) { $tl.LastUpdatedTime } else { $now }\n" +
            "            $diffMs = [long]($now - $lastUpd).TotalMilliseconds\n" +
            "            if (-not $cachedTitle -or ($nowMs - $lastMediaCheck) -gt 1500 -or ($dur -ne $lastDur -and $dur -gt 0)) {\n" +
            "                $lastMediaCheck = $nowMs\n" +
            "                $lastDur = $dur\n" +
            "                try {\n" +
            "                    $media = Await-WinRT ($curr.TryGetMediaPropertiesAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties])\n" +
            "                    if ($media -and $media.Title) {\n" +
            "                        $cachedTitle = $media.Title.Trim()\n" +
            "                        $cachedArtist = if ($media.Artist) { $media.Artist.Trim() } else { '' }\n" +
            "                        $cachedAlbum = if ($media.AlbumTitle) { $media.AlbumTitle.Trim() } else { '' }\n" +
            "                        $cachedSourceApp = if ($curr.SourceAppUserModelId) { $curr.SourceAppUserModelId.Trim() } else { '' }\n" +
            "                    }\n" +
            "                } catch {}\n" +
            "            }\n" +
            "            $currentTrackKey = \"$cachedArtist - $cachedTitle\"\n" +
            "            $trackChanged = ($currentTrackKey -ne $lastTrackKey)\n" +
            "            if ($trackChanged) {\n" +
            "                $lastTrackKey = $currentTrackKey\n" +
            "            }\n" +
            "            $statusChanged = ($status -ne $lastStatus)\n" +
            "            $pos = if ($status -eq 'Playing') {\n" +
            "                $rawPos + [Math]::Max(0, $diffMs)\n" +
            "            } else {\n" +
            "                $rawPos\n" +
            "            }\n" +
            "            if ($dur -gt 0 -and $pos -gt $dur) { $pos = $dur }\n" +
            "            if ($pos -lt 0) { $pos = 0 }\n" +
            "            $needsSend = $trackChanged -or $statusChanged -or (($nowMs - $lastSentTime) -ge 100)\n" +
            "            if ($needsSend) {\n" +
            "                $lastSentTime = $nowMs\n" +
            "                $lastStatus = $status\n" +
            "                if ($cachedTitle) {\n" +
            "                    [Console]::Out.WriteLine(\"LP|$status|$pos|$dur|$cachedArtist|$cachedTitle|$cachedAlbum|$cachedSourceApp\")\n" +
            "                    [Console]::Out.Flush()\n" +
            "                } else {\n" +
            "                    [Console]::Out.WriteLine('LP|NONE|0|0||||')\n" +
            "                    [Console]::Out.Flush()\n" +
            "                }\n" +
            "            }\n" +
            "        } else {\n" +
            "            if ($lastStatus -ne 'NONE' -or (($nowMs - $lastSentTime) -ge 1500)) {\n" +
            "                $lastStatus = 'NONE'\n" +
            "                $lastSentTime = $nowMs\n" +
            "                $lastTrackKey = ''\n" +
            "                $cachedTitle = ''\n" +
            "                $cachedArtist = ''\n" +
            "                $cachedAlbum = ''\n" +
            "                $cachedSourceApp = ''\n" +
            "                [Console]::Out.WriteLine('LP|NONE|0|0||||')\n" +
            "                [Console]::Out.Flush()\n" +
            "            }\n" +
            "        }\n" +
            "    } catch {}\n" +
            "    Start-Sleep -Milliseconds 40\n" +
            "}\n";

    public void init() {
        if (running) return;
        running = true;

        Runtime.getRuntime().addShutdownHook(new Thread(this::close, "JNLyrics-ShutdownHook"));

        gsmtcThread = new Thread(this::runGsmtcBridge, "JNLyrics-GSMTC-Bridge");
        gsmtcThread.setDaemon(true);
        gsmtcThread.start();
    }

    private File ensureScript() {
        File dir = new File(FabricLoader.getInstance().getConfigDir().toFile(), "jnlyrics");
        if (!dir.exists()) dir.mkdirs();
        File script = new File(dir, "gsmtc_bridge.ps1");
        try (FileWriter writer = new FileWriter(script, StandardCharsets.UTF_8)) {
            writer.write(GSMTC_SCRIPT);
        } catch (Exception e) {
            // If locked by running powershell, existing file is present
        }
        return script;
    }

    private void runGsmtcBridge() {
        File scriptFile = ensureScript();
        while (running) {
            try {
                ProcessBuilder pb = new ProcessBuilder(
                        "powershell.exe",
                        "-NoProfile",
                        "-NonInteractive",
                        "-ExecutionPolicy", "Bypass",
                        "-File", scriptFile.getAbsolutePath()
                );
                pb.redirectErrorStream(true);
                gsmtcProcess = pb.start();

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(gsmtcProcess.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while (running && (line = reader.readLine()) != null) {
                        // Immediately drain any accumulated lines in the buffer so we always consume the freshest telemetry
                        while (reader.ready()) {
                            String next = reader.readLine();
                            if (next != null && next.startsWith("LP|")) {
                                line = next;
                            }
                        }
                        if (line.startsWith("LP|")) {
                            parseLine(line);
                        }
                    }
                }
            } catch (Exception e) {
                JNLyricsMod.LOGGER.warn("[JNLyrics] GSMTC process error: {}", e.getMessage());
            } finally {
                if (gsmtcProcess != null) {
                    try {
                        gsmtcProcess.destroyForcibly();
                    } catch (Exception ignored) {}
                        gsmtcProcess = null;
                }
            }

            if (running) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {}
            }
        }
    }

    private void parseLine(String line) {
        String[] parts = line.split("\\|", -1);
        if (parts.length >= 7) {
            String status = parts[1];
            long pos = parseLongSafe(parts[2]);
            long dur = parseLongSafe(parts[3]);
            String artist = parts[4];
            String title = parts[5];
            String album = parts[6];
            String sourceApp = parts.length >= 8 ? parts[7] : "";

            TrackInfo track = new TrackInfo(status, title, artist, album, sourceApp, pos, dur);
            currentTrack.set(track);

            String trackKey = track.getArtist() + " - " + track.getTitle() + " [" + status + "]";
            if (!trackKey.equals(lastLoggedTrack) && track.isValidTrack()) {
                lastLoggedTrack = trackKey;
                JNLyricsMod.LOGGER.info("[JNLyrics] Media terdeteksi ({}): {} - {} [{}]",
                        track.getSourceApp().isEmpty() ? "Unknown" : track.getSourceApp(),
                        track.getArtist(), track.getTitle(), status);
            }
        }
    }

    private long parseLongSafe(String s) {
        try {
            return Long.parseLong(s);
        } catch (Exception e) {
            return 0;
        }
    }

    public TrackInfo getCurrentTrack() {
        return currentTrack.get();
    }

    public void close() {
        running = false;
        if (gsmtcProcess != null) {
            try {
                gsmtcProcess.destroyForcibly();
            } catch (Exception ignored) {}
            gsmtcProcess = null;
        }
        if (gsmtcThread != null) {
            gsmtcThread.interrupt();
            gsmtcThread = null;
        }
    }
}
