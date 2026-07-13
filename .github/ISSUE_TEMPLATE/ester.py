#!/usr/bin/env python3
import WebSocket from 'ws';
def validar_entero(mensaje, minimo, maximo):
    """Valida que el usuario ingrese un número dentro del rango"""
    while True:
        try:
            valor = int(input(mensaje))
            if valor < minimo or valor > maximo:
                print(f"❌ Error: Debes ingresar un número entre {minimo} y {maximo}.")
            else:
                return valor
        except ValueError:
            print("❌ Error: Ingresa solo números enteros.")
def mostrar_menu():
    print("\n" + "="*60)
    print("MÉXICO SEXTING: 💧💥")
    print("="*60)
    print("( Incluye chat, fotos, videos y videomensajes en vivo )\n")
    print("🔥 SEXTING")
    print("1. 10 min → $400")
    print("2. 20 min → $700")
    print("3. 30 min → $900\n")
    print("🔥 VIDEOLLAMADAS")
    print("4. 3 min  → $250")
    print("5. 5 min  → $350")
    print("6. 7 min  → $550")
    print("7. 10 min → $650\n")
    print("📌 ADICIONALES")
   print("📌 ADICIONALES")
    print("8.  Juguetes          → $150")
    print("9.  Lencería          → $100")
    print("10. Pies              → $200")
    print("11. Anal              → $300")
    print("12. Doble penetración → $500")
    print("13. Dominación        → $600")
    print("14. Sumisión          → $800")
    print("0.  Finalizar y pagar")
    print("="*60)
def calcular_total():
    mostrar_menu()

    print("\nSelecciona el servicio principal:")
    print("1 → Sexting")
    print("2 → Videollamada")
    tipo = validar_entero("Ingresa 1 o 2: ", 1, 2)

    if tipo == 1:
        print("\nDuración del Sexting:")
        print("1. 10 min ($400)")
        print("2. 20 min ($700)")
        print("3. 30 min ($900)")
        opcion = validar_entero("Elige (1-3): ", 1, 3)
        precios = {1: 400, 2: 700, 3: 900}
        total = precios[opcion]
        servicio = f"Sexting {opcion*10} min"
    else:
        print("\nDuración de la Videollamada:")
        print("1. 3 min  ($250)")
        print("2. 5 min  ($350)")
        print("3. 7 min  ($550)")
        print("4. 10 min ($650)")
        opcion = validar_entero("Elige (1-4): ", 1, 4)
        precios = {1: 250, 2: 350, 3: 550, 4: 650}
        total = precios[opcion]
        servicio = f"Videollamada {[3,5,7,10][opcion-1]} min"
    # Adicionales
    adicionales = {
        8: ("Juguetes", 150), 9: ("Lencería", 100), 10: ("Pies", 200),
        11: ("Anal", 300), 12: ("Doble penetración", 500),
        13: ("Dominación", 600), 14: ("Sumisión", 800)
    }

    total_adicionales = 0
    seleccionados = []

    print("\nAgrega adicionales (0 para terminar):")
    while True:
        eleccion = validar_entero("Número (0-14): ", 0, 14)
        if eleccion == 0:
            break
        if eleccion in adicionales:
            nombre, precio = adicionales[eleccion]
            total_adicionales += precio
            seleccionados.append(nombre)
            print(f"✅ Agregado: {nombre} (+${precio})")
        else:
            print("❌ Opción inválida.")
    total_final = total + total_adicionales
    print("\n" + "="*60)
    print("RESUMEN FINAL")
    print("="*60)
    print(f"Servicio: {servicio}")
    print(f"Monto base: ${total}")
    if seleccionados:
        print("Adicionales:", ", ".join(seleccionados))
    print(f"TOTAL A PAGAR: ${total_final} MXN")
    print("="*60)

    return total_final
if __name__ == "__main__":
    print("💧 Sistema de México Sexting iniciado")
    while True:
        calcular_total()
        if input("\n¿Nuevo pedido? (s/n): ").strip().lower() != 's':
            print("¡Gracias! 💥")
            break