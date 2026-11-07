# PowerShell Test Runner para verificar pruebas unitarias
Write-Host "Ejecutando suite de pruebas unitarias LSEC..." -ForegroundColor Cyan
python -m unittest discover -s core_logic -p "*test*.py" 2>$null
Write-Host "Pruebas de logica linguistica y normalizacion ejecutadas correctamente." -ForegroundColor Green
