from pathlib import Path

p = Path("src/com/gass/gassnative/MainActivity.java")
s = p.read_text()

start_marker = '''        "cat > /usr/local/bin/gass-windows-app <<'''
end_marker = '''        "echo 'GASS-WINE Runtime and shortcut ready.'''

start = s.find(start_marker)
end = s.find(end_marker, start)

if start == -1 or end == -1:
    raise SystemExit("ERROR: markers not found; Java source left unchanged.")

fixed = r'''        "cat > /usr/local/bin/gass-windows-app <<'GASS_WINE_EOF'\n" +
        "#!/bin/bash\n" +
        "export HOME=/home/user WINEPREFIX=/home/user/.gass-wine DISPLAY=:1\n" +
        "export HODLL64=libarm64ecfex.dll HODLL=libwow64fex.dll\n" +
        "export LIBGL_ALWAYS_SOFTWARE=1 GALLIUM_DRIVER=llvmpipe\n" +
        "unset VK_ICD_FILENAMES MESA_LOADER_DRIVER_OVERRIDE TURNIP_KMD FD_FORCE_KGSL\n" +
        "if [ -n \"${1:-}\" ]; then exec wine \"$1\"; fi\n" +
        "APP=$(zenity --file-selection --title='Choose a Windows application' --file-filter='Windows applications (*.exe) | *.exe') || exit 0\n" +
        "[ -n \"$APP\" ] && exec wine \"$APP\"\n" +
        "GASS_WINE_EOF\n" +
        "chmod 755 /usr/local/bin/gass-windows-app\n" +
        "mkdir -p /usr/share/applications /home/user/.local/share/applications /home/user/Desktop\n" +
        "cat > /usr/share/applications/gass-windows-exe.desktop <<'GASS_EXE_EOF'\n" +
        "[Desktop Entry]\n" +
        "Type=Application\n" +
        "Name=Windows Application (Hangover)\n" +
        "Exec=/usr/local/bin/gass-windows-app %f\n" +
        "Icon=application-x-ms-dos-executable\n" +
        "Terminal=false\n" +
        "MimeType=application/x-ms-dos-executable;application/x-msdownload;\n" +
        "Categories=System;\n" +
        "GASS_EXE_EOF\n" +
        "cat > /home/user/Desktop/Windows-Apps.desktop <<'GASS_DESKTOP_EOF'\n" +
        "[Desktop Entry]\n" +
        "Version=1.0\n" +
        "Type=Application\n" +
        "Name=Windows Apps\n" +
        "Exec=/usr/local/bin/gass-windows-app\n" +
        "Icon=application-x-ms-dos-executable\n" +
        "Terminal=false\n" +
        "Categories=System;\n" +
        "GASS_DESKTOP_EOF\n" +
        "cat > /home/user/.local/share/applications/mimeapps.list <<'GASS_MIME_EOF'\n" +
        "[Default Applications]\n" +
        "application/x-ms-dos-executable=gass-windows-exe.desktop\n" +
        "application/x-msdownload=gass-windows-exe.desktop\n" +
        "\n[Added Associations]\n" +
        "application/x-ms-dos-executable=gass-windows-exe.desktop;\n" +
        "application/x-msdownload=gass-windows-exe.desktop;\n" +
        "GASS_MIME_EOF\n" +
        "chown -R user:user /home/user/.local /home/user/Desktop\n" +
        "chmod 755 /home/user/Desktop/Windows-Apps.desktop\n" +
'''

p.write_text(s[:start] + fixed + s[end:])
print("PATCH_OK")
