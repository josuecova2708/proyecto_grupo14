    #!/bin/bash
# Instala el cron job que ejecuta MoronCorreo cada 5 minutos.
# Ejecutar UNA SOLA VEZ en el servidor: bash setup_cron.sh

CRON_LINE="*/5 * * * * $HOME/run.sh"

# Agrega la línea solo si no existe ya
if crontab -l 2>/dev/null | grep -qF "run.sh"; then
    echo "El cron job ya estaba instalado:"
else
    (crontab -l 2>/dev/null; echo "$CRON_LINE") | crontab -
    echo "Cron job instalado:"
fi

crontab -l
