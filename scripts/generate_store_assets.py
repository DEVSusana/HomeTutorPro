import os
from PIL import Image, ImageDraw, ImageFont, ImageFilter

OUTPUT_DIR = "docs/screenshots/play_store_ready"
RAW_DIR = "docs/screenshots/desktop_raw"
BASE_DIR = "docs/screenshots"

os.makedirs(OUTPUT_DIR, exist_ok=True)

# Fonts
FONT_HEAVY = ImageFont.truetype('/System/Library/Fonts/Avenir Next.ttc', 66, index=8)
FONT_BOLD = ImageFont.truetype('/System/Library/Fonts/Avenir Next.ttc', 56, index=0)
FONT_DEMI = ImageFont.truetype('/System/Library/Fonts/Avenir Next.ttc', 34, index=2)
FONT_BADGE = ImageFont.truetype('/System/Library/Fonts/Avenir Next.ttc', 26, index=0)

FONT_FG_TITLE = ImageFont.truetype('/System/Library/Fonts/Avenir Next.ttc', 56, index=8)
FONT_FG_SUB = ImageFont.truetype('/System/Library/Fonts/Avenir Next.ttc', 24, index=2)
FONT_FG_BADGE = ImageFont.truetype('/System/Library/Fonts/Avenir Next.ttc', 17, index=0)

# Colors
COLOR_BG_START = (15, 23, 42)    # Slate 900
COLOR_BG_END = (30, 27, 75)      # Indigo 950
COLOR_ACCENT = (99, 102, 241)    # Indigo 500
COLOR_EMERALD = (52, 211, 153)   # Emerald 400
COLOR_TEXT_PRIMARY = (255, 255, 255)
COLOR_TEXT_SECONDARY = (203, 213, 225) # Slate 300

def draw_vertical_gradient(draw, width, height, start_color, end_color):
    for y in range(height):
        ratio = y / height
        r = int(start_color[0] + (end_color[0] - start_color[0]) * ratio)
        g = int(start_color[1] + (end_color[1] - start_color[1]) * ratio)
        b = int(start_color[2] + (end_color[2] - start_color[2]) * ratio)
        draw.line([(0, y), (width, y)], fill=(r, g, b))

def draw_pill_badge(draw, text, center_x, top_y, badge_font, accent_color=COLOR_EMERALD):
    bbox = draw.textbbox((0, 0), text, font=badge_font)
    text_w = bbox[2] - bbox[0]
    text_h = bbox[3] - bbox[1]
    pad_x, pad_y = 22, 10
    box_w = text_w + pad_x * 2
    box_h = text_h + pad_y * 2
    x0 = center_x - box_w // 2
    y0 = top_y
    x1 = x0 + box_w
    y1 = y0 + box_h
    
    draw.rounded_rectangle([(x0, y0), (x1, y1)], radius=box_h // 2, fill=(30, 41, 59), outline=accent_color, width=2)
    draw.text((x0 + pad_x, y0 + pad_y - 2), text, font=badge_font, fill=accent_color)
    return y1

def create_phone_mockup(raw_img_path, target_width=880, corner_radius=44):
    raw = Image.open(raw_img_path).convert('RGBA')
    aspect = raw.height / raw.width
    target_height = int(target_width * aspect)
    resized = raw.resize((target_width, target_height), Image.Resampling.LANCZOS)
    
    mask = Image.new('L', (target_width, target_height), 0)
    draw_mask = ImageDraw.Draw(mask)
    draw_mask.rounded_rectangle([(0, 0), (target_width, target_height)], corner_radius, fill=255)
    
    screen = Image.new('RGBA', (target_width, target_height), (0, 0, 0, 0))
    screen.paste(resized, (0, 0), mask=mask)
    
    border = 12
    chassis_w = target_width + border * 2
    chassis_h = target_height + border * 2
    chassis = Image.new('RGBA', (chassis_w, chassis_h), (0, 0, 0, 0))
    draw_chassis = ImageDraw.Draw(chassis)
    
    draw_chassis.rounded_rectangle(
        [(0, 0), (chassis_w, chassis_h)],
        corner_radius + border,
        fill=(15, 23, 42, 255),
        outline=(71, 85, 105, 220),
        width=3
    )
    chassis.paste(screen, (border, border), mask=screen)
    
    # Shadow
    shadow_pad = 60
    shadow_w = chassis_w + shadow_pad * 2
    shadow_h = chassis_h + shadow_pad * 2
    shadow = Image.new('RGBA', (shadow_w, shadow_h), (0, 0, 0, 0))
    draw_shadow = ImageDraw.Draw(shadow)
    draw_shadow.rounded_rectangle(
        [(shadow_pad, shadow_pad + 20), (shadow_pad + chassis_w, shadow_pad + chassis_h + 20)],
        corner_radius + border,
        fill=(0, 0, 0, 180)
    )
    shadow = shadow.filter(ImageFilter.GaussianBlur(radius=30))
    shadow.paste(chassis, (shadow_pad, shadow_pad), mask=chassis)
    return shadow

def generate_portrait_slide(badge, title_lines, subtitle_lines, raw_img_path, output_filename, badge_color=COLOR_EMERALD, mockup_width=860, mockup_y=540):
    W, H = 1080, 2400
    canvas = Image.new('RGB', (W, H))
    draw = ImageDraw.Draw(canvas)
    
    # Background gradient
    draw_vertical_gradient(draw, W, H, COLOR_BG_START, COLOR_BG_END)
    
    # Decorative subtle circles in background
    glow = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    draw_glow = ImageDraw.Draw(glow)
    draw_glow.ellipse([(W//2 - 400, 300), (W//2 + 400, 1100)], fill=(79, 70, 229, 35))
    glow = glow.filter(ImageFilter.GaussianBlur(radius=100))
    canvas.paste(Image.new('RGB', (W, H), COLOR_BG_START), (0, 0))
    draw_vertical_gradient(draw, W, H, COLOR_BG_START, COLOR_BG_END)
    canvas.paste(glow, (0, 0), mask=glow)
    draw = ImageDraw.Draw(canvas)
    
    # 1. Pill Badge
    cur_y = 110
    badge_bottom = draw_pill_badge(draw, badge, W // 2, cur_y, FONT_BADGE, accent_color=badge_color)
    
    # 2. Main Title (Centered)
    cur_y = badge_bottom + 30
    for line in title_lines:
        bbox = draw.textbbox((0, 0), line, font=FONT_HEAVY)
        w = bbox[2] - bbox[0]
        draw.text(((W - w) // 2, cur_y), line, font=FONT_HEAVY, fill=COLOR_TEXT_PRIMARY)
        cur_y += (bbox[3] - bbox[1]) + 14
        
    # 3. Subtitle (Centered)
    cur_y += 6
    for line in subtitle_lines:
        bbox = draw.textbbox((0, 0), line, font=FONT_DEMI)
        w = bbox[2] - bbox[0]
        draw.text(((W - w) // 2, cur_y), line, font=FONT_DEMI, fill=COLOR_TEXT_SECONDARY)
        cur_y += (bbox[3] - bbox[1]) + 10
        
    # 4. Mockup (Bottom anchored, smoothly emerging)
    mockup = create_phone_mockup(raw_img_path, target_width=mockup_width, corner_radius=44)
    # Center horizontally
    mockup_x = (W - mockup.width) // 2
    # Paste mockup
    canvas.paste(mockup, (mockup_x, mockup_y), mask=mockup)
    
    out_path = os.path.join(OUTPUT_DIR, output_filename)
    canvas.save(out_path, quality=95)
    print(f"Generated: {out_path}")

def generate_horizontal_schedule_portrait(badge, title_lines, subtitle_lines, raw_img_path, output_filename):
    """Special portrait slide highlighting the horizontal schedule phone mockup!"""
    W, H = 1080, 2400
    canvas = Image.new('RGB', (W, H))
    draw = ImageDraw.Draw(canvas)
    draw_vertical_gradient(draw, W, H, COLOR_BG_START, COLOR_BG_END)
    
    # Pill Badge
    cur_y = 110
    badge_bottom = draw_pill_badge(draw, badge, W // 2, cur_y, FONT_BADGE, accent_color=COLOR_EMERALD)
    
    # Title
    cur_y = badge_bottom + 30
    for line in title_lines:
        bbox = draw.textbbox((0, 0), line, font=FONT_HEAVY)
        w = bbox[2] - bbox[0]
        draw.text(((W - w) // 2, cur_y), line, font=FONT_HEAVY, fill=COLOR_TEXT_PRIMARY)
        cur_y += (bbox[3] - bbox[1]) + 14
        
    # Subtitle
    cur_y += 6
    for line in subtitle_lines:
        bbox = draw.textbbox((0, 0), line, font=FONT_DEMI)
        w = bbox[2] - bbox[0]
        draw.text(((W - w) // 2, cur_y), line, font=FONT_DEMI, fill=COLOR_TEXT_SECONDARY)
        cur_y += (bbox[3] - bbox[1]) + 10

    # Horizontal mockup in landscape orientation (rotated or framed wide)
    # The raw image is 2460x1080. We can show it as a horizontal device across the card!
    target_w = 980
    raw = Image.open(raw_img_path).convert('RGBA')
    aspect = raw.height / raw.width
    target_h = int(target_w * aspect)
    resized = raw.resize((target_w, target_h), Image.Resampling.LANCZOS)
    
    corner_radius = 28
    mask = Image.new('L', (target_w, target_h), 0)
    draw_mask = ImageDraw.Draw(mask)
    draw_mask.rounded_rectangle([(0, 0), (target_w, target_h)], corner_radius, fill=255)
    
    screen = Image.new('RGBA', (target_w, target_h), (0, 0, 0, 0))
    screen.paste(resized, (0, 0), mask=mask)
    
    border = 12
    cw = target_w + border * 2
    ch = target_h + border * 2
    chassis = Image.new('RGBA', (cw, ch), (0, 0, 0, 0))
    draw_chassis = ImageDraw.Draw(chassis)
    draw_chassis.rounded_rectangle([(0, 0), (cw, ch)], corner_radius + border, fill=(15, 23, 42, 255), outline=(71, 85, 105, 220), width=3)
    chassis.paste(screen, (border, border), mask=screen)
    
    # Shadow
    sp = 50
    shadow = Image.new('RGBA', (cw + sp * 2, ch + sp * 2), (0, 0, 0, 0))
    draw_sh = ImageDraw.Draw(shadow)
    draw_sh.rounded_rectangle([(sp, sp + 15), (sp + cw, sp + ch + 15)], corner_radius + border, fill=(0, 0, 0, 180))
    shadow = shadow.filter(ImageFilter.GaussianBlur(radius=28))
    shadow.paste(chassis, (sp, sp), mask=chassis)
    
    # Paste primary landscape mockup in center
    mockup_x = (W - shadow.width) // 2
    mockup_y = 620
    canvas.paste(shadow, (mockup_x, mockup_y), mask=shadow)
    
    # Feature Callout Cards below mockup
    cards_y = mockup_y + shadow.height + 60
    callouts = [
        ("⚡ 7 DÍAS EN PANTALLA", "Visión completa de lunes a domingo sin scroll vertical"),
        ("🛡️ CERO SOLAPAMIENTOS", "Detecta huecos libres y conflictos de horario al instante"),
        ("📅 VISTA EN UN TOQUE", "Alterna a la cuadrícula apaisada fácilmente cuando lo necesites")
    ]
    card_w = 920
    card_h = 130
    for idx, (head, desc) in enumerate(callouts):
        cy = cards_y + idx * (card_h + 30)
        cx = (W - card_w) // 2
        # Card background
        draw.rounded_rectangle([(cx, cy), (cx + card_w, cy + card_h)], radius=20, fill=(30, 41, 59, 220), outline=(71, 85, 105, 150), width=2)
        # Text
        draw.text((cx + 36, cy + 26), head, font=FONT_BADGE, fill=COLOR_EMERALD)
        draw.text((cx + 36, cy + 68), desc, font=FONT_DEMI, fill=COLOR_TEXT_PRIMARY)
        
    out_path = os.path.join(OUTPUT_DIR, output_filename)
    canvas.save(out_path, quality=95)
    print(f"Generated: {out_path}")

def generate_landscape_slide(badge, title, subtitle, raw_img_path, output_filename):
    """1920x1080 Landscape Screenshot for Tablets, Chromebooks and Google Play Wide Listing!"""
    W, H = 1920, 1080
    canvas = Image.new('RGB', (W, H))
    draw = ImageDraw.Draw(canvas)
    draw_vertical_gradient(draw, W, H, COLOR_BG_START, COLOR_BG_END)
    
    # Left column: Text & Features (width ~ 680)
    # Right column: Landscape mockup (width ~ 1100)
    cur_y = 120
    badge_bottom = draw_pill_badge(draw, badge, 380, cur_y, FONT_BADGE, accent_color=COLOR_EMERALD)
    
    # Title
    cur_y = badge_bottom + 30
    title_lines = [
        "HORARIO SEMANAL",
        "PANORÁMICO"
    ]
    for line in title_lines:
        draw.text((90, cur_y), line, font=FONT_HEAVY, fill=COLOR_TEXT_PRIMARY)
        cur_y += 76
        
    cur_y += 10
    subtitle_lines = [
        "Visualiza tu semana completa",
        "de lunes a domingo en un grid",
        "optimizado sin solapamientos."
    ]
    for line in subtitle_lines:
        draw.text((90, cur_y), line, font=FONT_DEMI, fill=COLOR_TEXT_SECONDARY)
        cur_y += 44
        
    # Badges / benefits
    cur_y += 30
    benefits = [
        "✓ Vista total de 7 días al instante",
        "✓ Detección inteligente de huecos",
        "✓ Cuadrícula amplia, limpia y despejada"
    ]
    for ben in benefits:
        draw.text((90, cur_y), ben, font=FONT_DEMI, fill=COLOR_EMERALD)
        cur_y += 44
        
    # Right side: Landscape Mockup
    mockup_w = 1060
    raw = Image.open(raw_img_path).convert('RGBA')
    aspect = raw.height / raw.width
    mockup_h = int(mockup_w * aspect)
    resized = raw.resize((mockup_w, mockup_h), Image.Resampling.LANCZOS)
    
    corner_radius = 24
    mask = Image.new('L', (mockup_w, mockup_h), 0)
    draw_mask = ImageDraw.Draw(mask)
    draw_mask.rounded_rectangle([(0, 0), (mockup_w, mockup_h)], corner_radius, fill=255)
    
    screen = Image.new('RGBA', (mockup_w, mockup_h), (0, 0, 0, 0))
    screen.paste(resized, (0, 0), mask=mask)
    
    border = 12
    cw = mockup_w + border * 2
    ch = mockup_h + border * 2
    chassis = Image.new('RGBA', (cw, ch), (0, 0, 0, 0))
    draw_chassis = ImageDraw.Draw(chassis)
    draw_chassis.rounded_rectangle([(0, 0), (cw, ch)], corner_radius + border, fill=(15, 23, 42, 255), outline=(71, 85, 105, 220), width=3)
    chassis.paste(screen, (border, border), mask=screen)
    
    # Shadow
    sp = 50
    shadow = Image.new('RGBA', (cw + sp * 2, ch + sp * 2), (0, 0, 0, 0))
    draw_sh = ImageDraw.Draw(shadow)
    draw_sh.rounded_rectangle([(sp, sp + 20), (sp + cw, sp + ch + 20)], corner_radius + border, fill=(0, 0, 0, 200))
    shadow = shadow.filter(ImageFilter.GaussianBlur(radius=32))
    shadow.paste(chassis, (sp, sp), mask=chassis)
    
    mockup_x = W - shadow.width + 30
    mockup_y = (H - shadow.height) // 2
    canvas.paste(shadow, (mockup_x, mockup_y), mask=shadow)
    
    out_path = os.path.join(OUTPUT_DIR, output_filename)
    canvas.save(out_path, quality=95)
    print(f"Generated: {out_path}")

def generate_feature_graphic():
    """1024x500 Feature Graphic required by Google Play Console."""
    W, H = 1024, 500
    canvas = Image.new('RGB', (W, H))
    draw = ImageDraw.Draw(canvas)
    draw_vertical_gradient(draw, W, H, COLOR_BG_START, COLOR_BG_END)
    
    # Subtle glow in background
    glow = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    draw_glow = ImageDraw.Draw(glow)
    draw_glow.ellipse([(W - 400, -100), (W + 200, 500)], fill=(79, 70, 229, 60))
    glow = glow.filter(ImageFilter.GaussianBlur(radius=80))
    canvas.paste(glow, (0, 0), mask=glow)
    draw = ImageDraw.Draw(canvas)
    
    # App Icon (Left)
    icon_path = os.path.join(BASE_DIR, "ic_app_icon.png")
    if os.path.exists(icon_path):
        icon = Image.open(icon_path).convert('RGBA')
        icon = icon.resize((96, 96), Image.Resampling.LANCZOS)
        # Rounded icon
        mask = Image.new('L', (96, 96), 0)
        draw_m = ImageDraw.Draw(mask)
        draw_m.rounded_rectangle([(0, 0), (96, 96)], 22, fill=255)
        canvas.paste(icon, (60, 60), mask=mask)
        
    # Pill
    draw_pill_badge(draw, "⚡ IA ON-DEVICE · 100% OFFLINE", 360, 60, FONT_FG_BADGE, accent_color=COLOR_EMERALD)
    
    # Title & Subtitle
    draw.text((60, 180), "HomeTutorPro", font=FONT_FG_TITLE, fill=COLOR_TEXT_PRIMARY)
    draw.text((60, 255), "La app definitiva para profesores particulares", font=FONT_FG_SUB, fill=COLOR_TEXT_SECONDARY)
    draw.text((60, 290), "Horario semanal, gestión de alumnos, finanzas y asistente con IA local.", font=FONT_FG_SUB, fill=(148, 163, 184))
    
    # Badges arranged in 2x2 grid on the left to avoid touching the phone mockup
    draw_pill_badge(draw, "🔒 100% Privada", 175, 345, FONT_FG_BADGE, accent_color=COLOR_ACCENT)
    draw_pill_badge(draw, "📅 Vista Panorámica", 420, 345, FONT_FG_BADGE, accent_color=COLOR_ACCENT)
    draw_pill_badge(draw, "🧭 Navegación GPS", 175, 405, FONT_FG_BADGE, accent_color=COLOR_ACCENT)
    draw_pill_badge(draw, "📱 Widget Android", 420, 405, FONT_FG_BADGE, accent_color=COLOR_ACCENT)
        
    # Right Side: Emerging Phone Mockup
    raw_path = os.path.join(RAW_DIR, "Screenshot_20261025_125301.png")
    if os.path.exists(raw_path):
        mockup = create_phone_mockup(raw_path, target_width=320, corner_radius=20)
        canvas.paste(mockup, (W - 360, 60), mask=mockup)
        
    out_path = os.path.join(OUTPUT_DIR, "00_feature_graphic_1024x500.jpg")
    canvas.save(out_path, quality=95)
    print(f"Generated: {out_path}")

def main():
    print("Generating Google Play Store Graphics for HomeTutorPro...")
    
    # 0. Feature Graphic 1024x500
    generate_feature_graphic()
    
    # 1. Dashboard
    generate_portrait_slide(
        badge="ORGANIZACIÓN DIARIA",
        title_lines=["TODO TU DÍA", "BAJO CONTROL"],
        subtitle_lines=["Tus clases de hoy, cobros pendientes", "y avisos importantes de un vistazo."],
        raw_img_path=f"{RAW_DIR}/Screenshot_20261025_125301.png",
        output_filename="01_dashboard.jpg",
        badge_color=COLOR_EMERALD,
        mockup_y=550
    )
    
    # 2. Horario Semanal Horizontal (Especialmente destacado)
    generate_horizontal_schedule_portrait(
        badge="VISTA PANORÁMICA DESTACADA",
        title_lines=["HORARIO SEMANAL", "EN HORIZONTAL"],
        subtitle_lines=["La semana completa de lunes a domingo", "de un solo vistazo, sin solapamientos."],
        raw_img_path=f"{RAW_DIR}/Screenshot_20261025_125401.png",
        output_filename="02_horario_horizontal.jpg"
    )
    
    # 3. SUE Asistente IA On-Device
    generate_portrait_slide(
        badge="IA ON-DEVICE 100% PRIVADA",
        title_lines=["SUE: TU ASISTENTE", "DE VOZ CON IA"],
        subtitle_lines=["Edge AI con Gemma 3 local.", "Sin nube, 100% offline y privacidad total."],
        raw_img_path=f"{RAW_DIR}/Screenshot_20261025_125318.png",
        output_filename="03_sue_ia.jpg",
        badge_color=COLOR_ACCENT,
        mockup_y=550
    )
    
    # 4. Widget de Inicio Glance
    generate_portrait_slide(
        badge="ACCESO RÁPIDO GLANCE",
        title_lines=["WIDGET EN TU", "PANTALLA DE INICIO"],
        subtitle_lines=["Consulta tus próximas clases y alumnos", "al instante sin necesidad de abrir la app."],
        raw_img_path=f"{RAW_DIR}/Screenshot_20261025_125528.png",
        output_filename="04_widget.jpg",
        badge_color=COLOR_EMERALD,
        mockup_y=550
    )
    
    # 5. Directorio de Alumnos
    generate_portrait_slide(
        badge="GESTIÓN DE ALUMNOS",
        title_lines=["DIRECTORIO Y", "FICHAS COMPLETAS"],
        subtitle_lines=["Nivel académico, asignaturas, estado", "y precios por hora personalizados."],
        raw_img_path=f"{RAW_DIR}/Screenshot_20261025_125414.png",
        output_filename="05_alumnos.jpg",
        badge_color=COLOR_EMERALD,
        mockup_y=550
    )
    
    # 6. Perfil Alumno con GPS
    generate_portrait_slide(
        badge="NAVEGACIÓN GPS INTEGRADA",
        title_lines=["RUTA DIRECTA A CASA", "DE TUS ALUMNOS"],
        subtitle_lines=["Abre la ubicación en Google Maps y", "gestiona notas privadas y contacto rápido."],
        raw_img_path=f"{RAW_DIR}/Screenshot_20261025_125440.png",
        output_filename="06_perfil_gps.jpg",
        badge_color=COLOR_ACCENT,
        mockup_y=550
    )
    
    # 7. Horario Semanal Clásico (Vertical)
    generate_portrait_slide(
        badge="PLANIFICACIÓN SEMANAL",
        title_lines=["DETECCIÓN DE", "HUECOS LIBRES"],
        subtitle_lines=["Organiza y añade nuevas sesiones", "sin conflictos de horario."],
        raw_img_path=f"{RAW_DIR}/Screenshot_20261025_125332.png",
        output_filename="07_horario_vertical.jpg",
        badge_color=COLOR_EMERALD,
        mockup_y=550
    )
    
    # 8. Control Financiero
    fin_path = f"{BASE_DIR}/finanzas_alumno.png"
    if os.path.exists(fin_path):
        generate_portrait_slide(
            badge="FINANZAS Y FACTURACIÓN",
            title_lines=["CONTROL DE COBROS", "Y SALDOS AL DÍA"],
            subtitle_lines=["Historial de pagos, deudas pendientes", "y tarifas por alumno sin perder un euro."],
            raw_img_path=fin_path,
            output_filename="08_finanzas_cobros.jpg",
            badge_color=COLOR_EMERALD,
            mockup_y=550
        )
        
    # 9. Landscape Slide (1920x1080) Horizontal View
    generate_landscape_slide(
        badge="VISTA PANORÁMICA COMPLETA",
        title="HORARIO SEMANAL PANORÁMICO",
        subtitle="Visualiza tu semana completa de lunes a domingo",
        raw_img_path=f"{RAW_DIR}/Screenshot_20261025_125401.png",
        output_filename="landscape_01_horario_panoramico.jpg"
    )
    
    print("\n✅ All Store Graphics generated successfully in docs/screenshots/play_store_v2/!")

if __name__ == "__main__":
    main()
