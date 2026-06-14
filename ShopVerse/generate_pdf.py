"""
ShopVerse Architecture Deep Dive — PDF Generator
Converts ShopVerse_Architecture_Deep_Dive.md to a polished PDF using reportlab.
"""

import re
import os
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.units import cm, mm
from reportlab.lib import colors
from reportlab.lib.enums import TA_LEFT, TA_CENTER, TA_JUSTIFY
from reportlab.platypus import (
    SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle,
    PageBreak, HRFlowable, Preformatted, KeepTogether
)
from reportlab.platypus.flowables import Flowable
from reportlab.lib.colors import HexColor


# ── Colour palette ────────────────────────────────────────────────────────────
C_PRIMARY    = HexColor('#1a237e')   # deep navy
C_ACCENT     = HexColor('#0277bd')   # bright blue
C_WARN_BG    = HexColor('#fff3e0')   # warm amber background
C_WARN_BORDER= HexColor('#e65100')   # orange border
C_CODE_BG    = HexColor('#f5f5f5')   # light grey code background
C_TABLE_HEAD = HexColor('#0d47a1')   # table header navy
C_TABLE_ALT  = HexColor('#e8eaf6')   # alternating row indigo-50
C_BORDER     = HexColor('#c5cae9')   # indigo-100 border
C_H1_BG      = HexColor('#e8eaf6')
C_H2_LINE    = HexColor('#3949ab')
C_H3_LINE    = HexColor('#5c6bc0')
C_TEXT       = HexColor('#212121')
C_SUBTEXT    = HexColor('#546e7a')
WHITE        = colors.white

PAGE_W, PAGE_H = A4
MARGIN_L = 2.0 * cm
MARGIN_R = 2.0 * cm
MARGIN_T = 2.2 * cm
MARGIN_B = 2.0 * cm
COL_W = PAGE_W - MARGIN_L - MARGIN_R


# ── Styles ────────────────────────────────────────────────────────────────────
def make_styles():
    base = getSampleStyleSheet()

    def ps(name, **kw):
        return ParagraphStyle(name, **kw)

    return {
        'title': ps('SV_Title',
            fontSize=26, leading=32, textColor=C_PRIMARY,
            fontName='Helvetica-Bold', alignment=TA_CENTER,
            spaceAfter=6),
        'subtitle': ps('SV_Subtitle',
            fontSize=11, leading=16, textColor=C_SUBTEXT,
            fontName='Helvetica', alignment=TA_CENTER,
            spaceAfter=20),
        'toc_head': ps('SV_TocHead',
            fontSize=14, leading=18, textColor=C_PRIMARY,
            fontName='Helvetica-Bold', spaceBefore=10, spaceAfter=6),
        'toc': ps('SV_Toc',
            fontSize=10, leading=16, textColor=C_ACCENT,
            fontName='Helvetica', leftIndent=10, spaceAfter=2),
        'h1': ps('SV_H1',
            fontSize=18, leading=24, textColor=WHITE,
            fontName='Helvetica-Bold',
            spaceBefore=18, spaceAfter=10),
        'h2': ps('SV_H2',
            fontSize=14, leading=20, textColor=C_PRIMARY,
            fontName='Helvetica-Bold',
            spaceBefore=14, spaceAfter=6),
        'h3': ps('SV_H3',
            fontSize=12, leading=17, textColor=C_ACCENT,
            fontName='Helvetica-Bold',
            spaceBefore=10, spaceAfter=4),
        'h4': ps('SV_H4',
            fontSize=10.5, leading=15, textColor=C_TEXT,
            fontName='Helvetica-Bold',
            spaceBefore=8, spaceAfter=3),
        'body': ps('SV_Body',
            fontSize=9.5, leading=14.5, textColor=C_TEXT,
            fontName='Helvetica', alignment=TA_JUSTIFY,
            spaceAfter=6),
        'code': ps('SV_Code',
            fontSize=7.8, leading=11.5, textColor=C_TEXT,
            fontName='Courier', leftIndent=6,
            spaceAfter=4),
        'warn': ps('SV_Warn',
            fontSize=9, leading=13.5, textColor=HexColor('#bf360c'),
            fontName='Helvetica', leftIndent=8, spaceAfter=3),
        'warn_head': ps('SV_WarnHead',
            fontSize=10, leading=14, textColor=HexColor('#bf360c'),
            fontName='Helvetica-Bold', spaceAfter=4),
        'table_head': ps('SV_TH',
            fontSize=8.5, leading=12, textColor=WHITE,
            fontName='Helvetica-Bold', alignment=TA_CENTER),
        'table_cell': ps('SV_TD',
            fontSize=8, leading=12, textColor=C_TEXT,
            fontName='Helvetica'),
        'table_code': ps('SV_TDCode',
            fontSize=7.5, leading=11, textColor=HexColor('#1a237e'),
            fontName='Courier'),
        'hr_head': ps('SV_HrHead',
            fontSize=9, leading=13, textColor=C_SUBTEXT,
            fontName='Helvetica-Oblique', alignment=TA_CENTER,
            spaceAfter=4),
    }


STYLES = make_styles()


# ── Helper flowables ──────────────────────────────────────────────────────────
class H1Block(Flowable):
    """Full-width navy banner for H1 headings."""
    def __init__(self, text, width):
        super().__init__()
        self.text = text
        self.width = width
        self.height = 36

    def draw(self):
        c = self.canv
        c.setFillColor(C_PRIMARY)
        c.rect(0, 0, self.width, self.height, fill=1, stroke=0)
        c.setFillColor(WHITE)
        c.setFont('Helvetica-Bold', 15)
        c.drawString(12, 11, self.text)

    def wrap(self, aW, aH):
        return self.width, self.height


class WarnBox(Flowable):
    """Orange-bordered warning box."""
    def __init__(self, items, width):
        super().__init__()
        self.items = items
        self.width = width
        # estimate height
        self.height = 26 + len(items) * 16

    def draw(self):
        c = self.canv
        c.setFillColor(C_WARN_BG)
        c.setStrokeColor(C_WARN_BORDER)
        c.setLineWidth(1.5)
        c.rect(0, 0, self.width, self.height, fill=1, stroke=1)
        c.setFillColor(HexColor('#bf360c'))
        c.setFont('Helvetica-Bold', 9.5)
        c.drawString(10, self.height - 16, '⚠  Things That Will Break in Production')
        c.setFont('Helvetica', 8.5)
        y = self.height - 30
        for item in self.items:
            # wrap long text
            c.drawString(14, y, f'•  {item[:110]}')
            if len(item) > 110:
                c.drawString(24, y - 11, item[110:220])
                y -= 11
            y -= 14

    def wrap(self, aW, aH):
        return self.width, self.height


def code_block(text):
    """Return list of styled code block flowables — chunked to fit on pages."""
    LINES_PER_CHUNK = 32
    lines = text.split('\n')
    chunks = ['\n'.join(lines[j:j+LINES_PER_CHUNK])
              for j in range(0, max(1, len(lines)), LINES_PER_CHUNK)]

    result = []
    for chunk_text in chunks:
        result.append(Table(
            [[Preformatted(chunk_text, STYLES['code'])]],
            colWidths=[COL_W],
            style=TableStyle([
                ('BACKGROUND', (0,0), (-1,-1), C_CODE_BG),
                ('BOX', (0,0), (-1,-1), 0.5, C_BORDER),
                ('LEFTPADDING', (0,0), (-1,-1), 8),
                ('RIGHTPADDING', (0,0), (-1,-1), 8),
                ('TOPPADDING', (0,0), (-1,-1), 6),
                ('BOTTOMPADDING', (0,0), (-1,-1), 6),
            ])
        ))
    return result


def make_table(headers, rows):
    """Build a styled data table."""
    head_row = [Paragraph(h, STYLES['table_head']) for h in headers]
    body_rows = []
    for row in rows:
        body_rows.append([
            Paragraph(str(cell), STYLES['table_cell']) for cell in row
        ])

    col_count = len(headers)
    col_width = COL_W / col_count

    tbl = Table(
        [head_row] + body_rows,
        colWidths=[col_width] * col_count,
        repeatRows=1,
    )
    style = [
        ('BACKGROUND', (0, 0), (-1, 0), C_TABLE_HEAD),
        ('TEXTCOLOR', (0, 0), (-1, 0), WHITE),
        ('FONTNAME', (0, 0), (-1, 0), 'Helvetica-Bold'),
        ('FONTSIZE', (0, 0), (-1, 0), 8.5),
        ('ALIGN', (0, 0), (-1, 0), 'CENTER'),
        ('GRID', (0, 0), (-1, -1), 0.4, C_BORDER),
        ('VALIGN', (0, 0), (-1, -1), 'TOP'),
        ('FONTNAME', (0, 1), (-1, -1), 'Helvetica'),
        ('FONTSIZE', (0, 1), (-1, -1), 8),
        ('LEFTPADDING', (0, 0), (-1, -1), 5),
        ('RIGHTPADDING', (0, 0), (-1, -1), 5),
        ('TOPPADDING', (0, 0), (-1, -1), 4),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 4),
    ]
    for i in range(1, len(body_rows) + 1):
        if i % 2 == 0:
            style.append(('BACKGROUND', (0, i), (-1, i), C_TABLE_ALT))

    tbl.setStyle(TableStyle(style))
    return tbl


def escape_xml(text):
    """Escape XML special chars for Paragraph."""
    return (text
        .replace('&', '&amp;')
        .replace('<', '&lt;')
        .replace('>', '&gt;')
        .replace('"', '&quot;')
    )


# ── Page template ─────────────────────────────────────────────────────────────
def on_page(canvas, doc):
    canvas.saveState()
    # Header line
    canvas.setStrokeColor(C_BORDER)
    canvas.setLineWidth(0.5)
    canvas.line(MARGIN_L, PAGE_H - MARGIN_T + 4*mm,
                PAGE_W - MARGIN_R, PAGE_H - MARGIN_T + 4*mm)
    # Footer
    canvas.setFont('Helvetica', 7.5)
    canvas.setFillColor(C_SUBTEXT)
    canvas.drawString(MARGIN_L, MARGIN_B - 8*mm,
                      'ShopVerse Architecture Deep Dive  ·  v1.0  ·  June 2026')
    canvas.drawRightString(PAGE_W - MARGIN_R, MARGIN_B - 8*mm,
                           f'Page {doc.page}')
    canvas.restoreState()


# ── Markdown parser → flowables ───────────────────────────────────────────────
def parse_md(md_path):
    with open(md_path, encoding='utf-8') as f:
        lines = f.readlines()

    story = []
    i = 0
    in_code = False
    code_buf = []
    in_table = False
    table_buf = []
    in_warn = False
    warn_items = []

    def flush_code():
        if code_buf:
            joined = ''.join(code_buf).rstrip()
            story.append(Spacer(1, 4))
            story.extend(code_block(joined))
            story.append(Spacer(1, 4))
        code_buf.clear()

    def flush_warn():
        if warn_items:
            story.append(Spacer(1, 4))
            story.append(WarnBox(warn_items[:], COL_W))
            story.append(Spacer(1, 8))
        warn_items.clear()

    def flush_table():
        nonlocal in_table
        if len(table_buf) >= 2:
            # row 0 = headers, row 1 = separator, row 2+ = data
            headers = [c.strip() for c in table_buf[0].split('|') if c.strip()]
            data_rows = []
            for row_line in table_buf[2:]:
                cells = [c.strip() for c in row_line.split('|')]
                cells = [c for c in cells if True]  # keep all (including empty edge cells)
                # trim leading/trailing empty
                while cells and cells[0] == '':
                    cells.pop(0)
                while cells and cells[-1] == '':
                    cells.pop()
                if len(cells) == len(headers):
                    data_rows.append(cells)
            if headers and data_rows:
                story.append(Spacer(1, 4))
                story.append(make_table(headers, data_rows))
                story.append(Spacer(1, 6))
        table_buf.clear()
        in_table = False

    # skip the front matter / title
    while i < len(lines):
        raw = lines[i]
        line = raw.rstrip('\n')
        stripped = line.strip()

        # ── code fence ────────────────────────────────────────────────────────
        if stripped.startswith('```'):
            if in_code:
                in_code = False
                flush_code()
            else:
                in_code = True
                # language label ignored
            i += 1
            continue

        if in_code:
            code_buf.append(line + '\n')
            i += 1
            continue

        # ── table row ─────────────────────────────────────────────────────────
        if stripped.startswith('|'):
            if in_warn:
                flush_warn()
                in_warn = False
            in_table = True
            table_buf.append(stripped)
            i += 1
            continue
        elif in_table:
            flush_table()

        # ── warning block  > ### ⚠️ ──────────────────────────────────────────
        if '⚠️' in stripped and stripped.startswith('>'):
            if in_warn:
                flush_warn()
            in_warn = True
            warn_items = []
            i += 1
            continue
        if in_warn:
            if stripped.startswith('>'):
                content = stripped.lstrip('> ').strip()
                # numbered items
                m = re.match(r'^\d+\.\s+\*\*(.*?)\*\*(.*)$', content)
                if m:
                    warn_items.append(m.group(1) + m.group(2))
                elif content:
                    warn_items.append(content)
                i += 1
                continue
            else:
                flush_warn()
                in_warn = False

        # ── horizontal rule ───────────────────────────────────────────────────
        if re.match(r'^-{3,}$', stripped):
            story.append(HRFlowable(width='100%', thickness=0.5,
                                    color=C_BORDER, spaceAfter=6, spaceBefore=6))
            i += 1
            continue

        # ── headings ──────────────────────────────────────────────────────────
        if stripped.startswith('# ') and not stripped.startswith('## '):
            text = stripped[2:].strip()
            # section headings like "# 1. RabbitMQ"
            story.append(Spacer(1, 8))
            story.append(H1Block(text, COL_W))
            story.append(Spacer(1, 6))
            i += 1
            continue

        if stripped.startswith('## '):
            text = stripped[3:].strip()
            story.append(Spacer(1, 6))
            story.append(HRFlowable(width='100%', thickness=1.5,
                                    color=C_H2_LINE, spaceAfter=2, spaceBefore=2))
            story.append(Paragraph(escape_xml(text), STYLES['h2']))
            i += 1
            continue

        if stripped.startswith('### '):
            text = stripped[4:].strip()
            story.append(Paragraph(escape_xml(text), STYLES['h3']))
            i += 1
            continue

        if stripped.startswith('#### '):
            text = stripped[5:].strip()
            story.append(Paragraph(escape_xml(text), STYLES['h4']))
            i += 1
            continue

        # ── blockquote / note ─────────────────────────────────────────────────
        if stripped.startswith('> '):
            content = stripped[2:].strip()
            # strip markdown bold
            content = re.sub(r'\*\*(.*?)\*\*', r'\1', content)
            story.append(Paragraph(escape_xml(content), STYLES['warn']))
            i += 1
            continue

        # ── bullet list ───────────────────────────────────────────────────────
        m = re.match(r'^[-*]\s+(.*)', stripped)
        if m:
            content = m.group(1)
            content = re.sub(r'\*\*(.*?)\*\*', r'<b>\1</b>', content)
            content = re.sub(r'`(.*?)`', r'<font name="Courier">\1</font>', content)
            story.append(Paragraph(
                f'&bull;&nbsp;&nbsp;{escape_xml_keep_tags(content)}',
                STYLES['body']))
            i += 1
            continue

        # ── numbered list ─────────────────────────────────────────────────────
        m = re.match(r'^(\d+)\.\s+(.*)', stripped)
        if m:
            num = m.group(1)
            content = m.group(2)
            content = re.sub(r'\*\*(.*?)\*\*', r'<b>\1</b>', content)
            content = re.sub(r'`(.*?)`', r'<font name="Courier">\1</font>', content)
            story.append(Paragraph(
                f'<b>{num}.</b>&nbsp;&nbsp;{escape_xml_keep_tags(content)}',
                STYLES['body']))
            i += 1
            continue

        # ── page break markers ────────────────────────────────────────────────
        if stripped == '---':
            i += 1
            continue

        # ── blank line ────────────────────────────────────────────────────────
        if not stripped:
            story.append(Spacer(1, 4))
            i += 1
            continue

        # ── normal paragraph ──────────────────────────────────────────────────
        content = stripped
        # inline formatting
        content = re.sub(r'\*\*(.*?)\*\*', r'<b>\1</b>', content)
        content = re.sub(r'\*(.*?)\*', r'<i>\1</i>', content)
        content = re.sub(r'`(.*?)`', r'<font name="Courier" color="#1a237e">\1</font>', content)
        # strip markdown links [text](url) → text
        content = re.sub(r'\[([^\]]+)\]\([^\)]+\)', r'\1', content)
        story.append(Paragraph(content, STYLES['body']))
        i += 1

    # flush anything remaining
    if in_code:
        flush_code()
    if in_table:
        flush_table()
    if in_warn:
        flush_warn()

    return story


def escape_xml_keep_tags(text):
    """Escape & and raw < > that aren't already part of tags we inserted."""
    # We've already inserted <b>, </b>, <font ...>, </font> — protect those
    # Strategy: escape &, then handle < > carefully
    # Since we control the tags, just escape & in the original text first
    text = text.replace('&', '&amp;')
    # Don't double-escape our injected tags
    return text


# ── Cover page ────────────────────────────────────────────────────────────────
def cover_page():
    story = []
    story.append(Spacer(1, 3*cm))

    # Big title
    story.append(Paragraph('ShopVerse', ParagraphStyle('CVTitle',
        fontSize=40, leading=48, textColor=C_PRIMARY,
        fontName='Helvetica-Bold', alignment=TA_CENTER)))
    story.append(Spacer(1, 0.3*cm))
    story.append(Paragraph('Technology Architecture', ParagraphStyle('CVSub1',
        fontSize=24, leading=30, textColor=C_ACCENT,
        fontName='Helvetica-Bold', alignment=TA_CENTER)))
    story.append(Paragraph('Deep Dive', ParagraphStyle('CVSub2',
        fontSize=24, leading=30, textColor=C_ACCENT,
        fontName='Helvetica', alignment=TA_CENTER)))

    story.append(Spacer(1, 1.5*cm))
    story.append(HRFlowable(width='60%', thickness=2,
                             color=C_ACCENT, hAlign='CENTER'))
    story.append(Spacer(1, 1.5*cm))

    meta_style = ParagraphStyle('CVMeta',
        fontSize=10, leading=16, textColor=C_SUBTEXT,
        fontName='Helvetica', alignment=TA_CENTER)

    story.append(Paragraph('Internals, Architecture &amp; Critical Concepts', meta_style))
    story.append(Spacer(1, 0.4*cm))
    story.append(Paragraph('for every technology in the ShopVerse stack', meta_style))
    story.append(Spacer(1, 2*cm))

    # Tech stack chips rendered as a table
    techs = [
        ['RabbitMQ', 'Kafka', 'PostgreSQL', 'Redis', 'MongoDB'],
        ['Cassandra', 'Elasticsearch', 'Neo4j', 'Prometheus', 'Docker'],
    ]
    chip_style = ParagraphStyle('CVChip',
        fontSize=9, leading=13, textColor=WHITE,
        fontName='Helvetica-Bold', alignment=TA_CENTER)

    for row in techs:
        tbl = Table(
            [[Paragraph(t, chip_style) for t in row]],
            colWidths=[COL_W / 5] * 5,
            style=TableStyle([
                ('BACKGROUND', (0, 0), (-1, -1), C_ACCENT),
                ('ROUNDEDCORNERS', [4]),
                ('TOPPADDING', (0, 0), (-1, -1), 6),
                ('BOTTOMPADDING', (0, 0), (-1, -1), 6),
                ('LEFTPADDING', (0, 0), (-1, -1), 4),
                ('RIGHTPADDING', (0, 0), (-1, -1), 4),
                ('GRID', (0, 0), (-1, -1), 0.5, WHITE),
            ])
        )
        story.append(tbl)
        story.append(Spacer(1, 0.3*cm))

    story.append(Spacer(1, 2*cm))
    story.append(Paragraph('Version 1.0 &nbsp;&nbsp;&bull;&nbsp;&nbsp; June 2026 &nbsp;&nbsp;&bull;&nbsp;&nbsp; For Internal Developer Use', ParagraphStyle('CVFooter',
        fontSize=9, textColor=C_SUBTEXT,
        fontName='Helvetica-Oblique', alignment=TA_CENTER)))
    story.append(PageBreak())
    return story


# ── Main ──────────────────────────────────────────────────────────────────────
def main():
    src = r'/sessions/relaxed-laughing-archimedes/mnt/ShopVerse/ShopVerse_Architecture_Deep_Dive.md'
    out = r'/sessions/relaxed-laughing-archimedes/mnt/ShopVerse/ShopVerse_Architecture_Deep_Dive.pdf'

    doc = SimpleDocTemplate(
        out,
        pagesize=A4,
        leftMargin=MARGIN_L,
        rightMargin=MARGIN_R,
        topMargin=MARGIN_T,
        bottomMargin=MARGIN_B,
        title='ShopVerse Architecture Deep Dive',
        author='ShopVerse Engineering',
        subject='ShopVerse Technology Architecture Reference',
    )

    story = []
    story.extend(cover_page())
    story.extend(parse_md(src))
    doc.build(story, onFirstPage=on_page, onLaterPages=on_page)
    import os as _os
    print(f"PDF written: {_os.path.getsize(out)/1024:.0f} KB")


if __name__ == '__main__':
    main()
