"""Generate bilingual, theme-aware README SVGs using only the Python standard library.

Run from any directory: python scripts/generate-readme-assets.py
The wallet artwork is the original Aether mark, kept in docs/images/aether-wallet.svg.
"""

from html import escape
from pathlib import Path
import xml.etree.ElementTree as ET


ROOT = Path(__file__).resolve().parents[1]
IMAGES = ROOT / "docs" / "images"
PALETTES = {
    "light": dict(bg="#F8FAF3", ink="#234331", muted="#52694E", grid="#DCE5D2",
                  line="#C4D4B7", card="#FFFFFF", accent="#527A43", soft="#E8F0DD",
                  warning="#FFF5D6", border="#DFC47B", amber="#654B20", gold="#F1CE68"),
    "dark": dict(bg="#14251E", ink="#E1ECD7", muted="#A9BEA1", grid="#2A4032",
                 line="#466049", card="#1D3328", accent="#B6D28C", soft="#284331",
                 warning="#302819", border="#796437", amber="#F0DCA6", gold="#D9B957"),
}
COPY = {
    "en": dict(
        hero_title="Your money,", hero_sub="in perspective.",
        eyebrow="PERSONAL FINANCE / ON YOUR COMPUTER",
        intro="Accounts, cards and investments.",
        intro2="A local history. A clearer everyday view.",
        note="connect → organize → keep", bank="Institutions", local="Local history", backup="Backup",
        footer="Windows desktop · SQLite on your computer · Pluggy bank connections",
        warning_label="USER DISCLAIMER", warning_badge="FOR APP USERS",
        warning_title="Aether organizes your finances. The decisions are yours.",
        warning_body="No financial advice. Verify your data, protect your credentials and keep backups.",
        warning_foot="Provided “as is”, with liability limited as permitted by law. Read the full notice below.",
        backup_label="CONTINUITY / WITH PRIVACY", backup_title="Your history goes with you. Protected by a password.",
        computer="Your computer", computer_sub="History and preferences", file_sub="Password-protected file",
        restore="Another installation", restore_sub="Restore with your password",
        backup_foot="Credentials stay out of the file. No password recovery or master key. Active SQLite storage is separate.",
    ),
    "pt-BR": dict(
        hero_title="Seu dinheiro,", hero_sub="em perspectiva.",
        eyebrow="FINANÇAS PESSOAIS / NO SEU COMPUTADOR",
        intro="Contas, cartões e investimentos.",
        intro2="Histórico local. Mais clareza no dia a dia.",
        note="conectar → organizar → guardar", bank="Instituições", local="Histórico local", backup="Backup",
        footer="Desktop Windows · SQLite no seu computador · Conexões bancárias via Pluggy",
        warning_label="AVISO DE RESPONSABILIDADE", warning_badge="PARA QUEM USA",
        warning_title="O Aether organiza suas finanças. As decisões são suas.",
        warning_body="Sem aconselhamento financeiro. Confira os dados, proteja suas credenciais e mantenha backups.",
        warning_foot="Fornecido “como está”, com responsabilidade limitada conforme a lei. Leia o aviso completo abaixo.",
        backup_label="CONTINUIDADE / COM PRIVACIDADE", backup_title="Seu histórico vai com você. Protegido por senha.",
        computer="Seu computador", computer_sub="Histórico e preferências", file_sub="Arquivo protegido por senha",
        restore="Outra instalação", restore_sub="Restaure com sua senha",
        backup_foot="Credenciais ficam fora do arquivo. Sem recuperação de senha ou chave mestra. O SQLite ativo é separado.",
    ),
}


def text(x, y, value, size=18, fill="ink", weight=400, extra=""):
    return (f'<text x="{x}" y="{y}" font-size="{size}" fill="{P[fill]}" '
            f'font-weight="{weight}" {extra}>{escape(value)}</text>')


def rect(x, y, w, h, fill="card", radius=14, stroke="line"):
    return (f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="{radius}" '
            f'fill="{P[fill]}" stroke="{P[stroke]}"/>')


def icon(kind, x, y, size=24):
    paths = {
        "bank": '<path d="m3 9 9-6 9 6M4 10h16M6 11v7m6-7v7m6-7v7M3 21h18M4 18h16"/>',
        "local": '<ellipse cx="12" cy="5" rx="8" ry="3"/><path d="M4 5v14c0 4 16 4 16 0V5M4 12c0 4 16 4 16 0"/>',
        "backup": '<rect x="4" y="10" width="16" height="12" rx="3"/><path d="M8 10V6a4 4 0 0 1 8 0v4m-4 5v3"/>',
        "computer": '<rect x="2" y="3" width="20" height="14" rx="2"/><path d="M8 22h8m-4-5v5"/>',
    }
    return (f'<g transform="translate({x} {y}) scale({size / 24})" fill="none" '
            f'stroke="{P["accent"]}" stroke-width="1.6" stroke-linecap="round" '
            f'stroke-linejoin="round">{paths[kind]}</g>')


def arrow(x1, x2, y):
    # A static arrow remains visible when motion is disabled or unsupported.
    return (f'<path d="M{x1} {y}Q{(x1+x2)/2} {y-6} {x2} {y}" fill="none" '
            f'stroke="{P["accent"]}" stroke-width="1.6" stroke-dasharray="4 6" class="flow"/>'
            f'<path d="m{x2-6} {y-5} 6 5-6 4" fill="none" stroke="{P["accent"]}" '
            'stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/>')


def svg(name, height, content, title, desc, grid=False):
    pattern = (f'<defs><pattern id="grid" width="24" height="24" patternUnits="userSpaceOnUse">'
               f'<path d="M24 0H0V24" fill="none" stroke="{P["grid"]}" stroke-width=".6"/>'
               '</pattern></defs>') if grid else ""
    body = (f'<svg xmlns="http://www.w3.org/2000/svg" width="1120" height="{height}" '
            f'viewBox="0 0 1120 {height}" role="img" aria-labelledby="title desc" xml:lang="{LANG}">'
            f'<title id="title">{escape(title)}</title><desc id="desc">{escape(desc)}</desc>'
            '<style>text{font-family:"Segoe UI",Arial,sans-serif}'
            '.flow{animation:drift 4s linear infinite}'
            '@keyframes drift{to{stroke-dashoffset:-40}}'
            '@media(prefers-reduced-motion:reduce){.flow{animation:none}}</style>'
            f'{pattern}{content}</svg>\n')
    ET.fromstring(body)
    (IMAGES / f"aether-{name}.{LANG}.{THEME}.svg").write_text(body, encoding="utf-8")


def hero():
    wallet = ET.parse(IMAGES / "aether-wallet.svg").getroot()
    wallet.set("x", "32")
    wallet.set("y", "25")
    wallet.set("width", "54")
    wallet.set("height", "48")
    content = rect(1, 1, 1118, 326, "bg", 20)
    content += '<rect x="1" y="1" width="1118" height="326" rx="20" fill="url(#grid)"/>'
    content += ET.tostring(wallet, encoding="unicode")
    content += text(98, 60, "aether", 30, weight=700, extra='letter-spacing="-1"')
    content += text(34, 103, C["eyebrow"], 11, "muted", 600, 'letter-spacing="2"')
    content += text(32, 157, C["hero_title"], 44, weight=700, extra='letter-spacing="-1.5"')
    content += text(32, 207, C["hero_sub"], 44, weight=700, extra='letter-spacing="-1.5"')
    content += text(35, 245, C["intro"], 18, "muted")
    content += text(35, 273, C["intro2"], 18, "muted")
    content += text(610, 112, C["note"], 17, "muted", extra='transform="rotate(-3 610 112)" font-style="italic"')
    for x, label, kind in [(610, C["bank"], "bank"), (779, C["local"], "local"), (948, C["backup"], "backup")]:
        content += rect(x, 146, 134, 102)
        content += icon(kind, x+53, 164, 28)
        content += text(x+67, 224, label, 15, weight=600, extra='text-anchor="middle"')
    content += arrow(750, 772, 195) + arrow(919, 941, 195)
    content += f'<path d="M615 262q220 10 460-3" fill="none" stroke="{P["accent"]}" opacity=".5" stroke-linecap="round"/>'
    content += text(35, 309, C["footer"], 13, "muted")
    svg("hero", 328, content, "Aether — " + C["hero_title"] + " " + C["hero_sub"], C["footer"], True)


def disclaimer():
    content = rect(1, 1, 1118, 138, "warning", 16, "border")
    content += f'<rect x="1" y="26" width="3" height="88" rx="1.5" fill="{P["gold"]}"/>'
    content += rect(24, 42, 56, 56, "gold", 16, "gold")
    content += ('<g fill="none" stroke="#493615" stroke-width="2.2" stroke-linejoin="round">'
                '<path d="M49 54a3.5 3.5 0 0 1 6 0l15 26a3.5 3.5 0 0 1-3 5H37a3.5 3.5 0 0 1-3-5Z"/>'
                '<path d="M52 64v9" stroke-linecap="round"/><circle cx="52" cy="79" r=".8"/></g>')
    content += text(104, 38, C["warning_label"], 12, "amber", 700, 'letter-spacing="2"')
    content += text(104, 68, C["warning_title"], 22, "amber", 700, 'letter-spacing="-.3"')
    content += text(104, 94, C["warning_body"], 16, "amber")
    content += text(104, 117, C["warning_foot"], 15, "amber")
    content += text(1084, 38, C["warning_badge"], 11, "amber", 600, 'text-anchor="end" letter-spacing="1"')
    svg("disclaimer", 140, content, C["warning_label"], C["warning_body"] + " " + C["warning_foot"])


def backup():
    content = rect(1, 1, 1118, 236, "bg", 20)
    content += '<rect x="1" y="1" width="1118" height="236" rx="20" fill="url(#grid)"/>'
    content += text(30, 32, C["backup_label"], 11, "muted", 600, 'letter-spacing="2"')
    content += text(30, 67, C["backup_title"], 26, weight=700, extra='letter-spacing="-.5"')
    for x, title, subtitle, kind in [(30, C["computer"], C["computer_sub"], "computer"),
                                       (426, ".aetherbackup", C["file_sub"], "backup"),
                                       (822, C["restore"], C["restore_sub"], "computer")]:
        content += rect(x, 91, 268, 94)
        content += icon(kind, x+18, 108, 24)
        content += text(x+55, 123, title, 18, weight=600)
        content += text(x+18, 160, subtitle, 15, "muted")
    content += arrow(315, 408, 134) + arrow(711, 804, 134)
    content += text(362, 119, "AES-256-GCM", 11, "muted", 600, 'text-anchor="middle"')
    content += text(30, 218, C["backup_foot"], 15, "muted")
    svg("backup", 238, content, C["backup_title"], C["backup_foot"], True)


if __name__ == "__main__":
    for LANG, C in COPY.items():
        for THEME, P in PALETTES.items():
            hero()
            disclaimer()
            backup()
    print("Generated 12 README SVGs (English / Portuguese, light / dark).")
