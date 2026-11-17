export type ResourceKind = 'strings' | 'colors' | 'dimens';

export interface AndroidResourceEntry {
  id: string;
  name: string;
  value: string;
}

export interface ResourceFileMeta {
  kind: ResourceKind;
  filename: string;
  defaultPath: string;
  title: string;
  tagName: 'string' | 'color' | 'dimen';
}

export const RESOURCE_FILES_META: ResourceFileMeta[] = [
  {
    kind: 'strings',
    filename: 'strings.xml',
    defaultPath: 'app/src/main/res/values/strings.xml',
    title: 'Строки (strings.xml)',
    tagName: 'string',
  },
  {
    kind: 'colors',
    filename: 'colors.xml',
    defaultPath: 'app/src/main/res/values/colors.xml',
    title: 'Цвета (colors.xml)',
    tagName: 'color',
  },
  {
    kind: 'dimens',
    filename: 'dimens.xml',
    defaultPath: 'app/src/main/res/values/dimens.xml',
    title: 'Размеры (dimens.xml)',
    tagName: 'dimen',
  },
];

export const DEFAULT_RESOURCE_XML: Record<ResourceKind, string> = {
  strings: `<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">DroidVoice Notes</string>
    <string name="voice_hint">Произнесите команду для генерации Kotlin кода</string>
    <string name="action_add_note">Добавить заметку</string>
    <string name="action_reset">Сбросить счетчик</string>
</resources>
`,
  colors: `<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="brand_primary">#10B981</color>
    <color name="brand_secondary">#38BDF8</color>
    <color name="surface_dark">#0B0F17</color>
    <color name="card_surface">#131A26</color>
    <color name="text_primary">#F8FAFC</color>
    <color name="error_crimson">#EF4444</color>
</resources>
`,
  dimens: `<?xml version="1.0" encoding="utf-8"?>
<resources>
    <dimen name="spacing_small">8dp</dimen>
    <dimen name="spacing_medium">16dp</dimen>
    <dimen name="spacing_large">24dp</dimen>
    <dimen name="card_corner_radius">14dp</dimen>
    <dimen name="title_text_size">18sp</dimen>
</resources>
`,
};

export function getResourceKindFromPath(filePath: string): ResourceKind | null {
  const lower = filePath.toLowerCase();
  if (lower === 'strings.xml' || lower.endsWith('/strings.xml')) return 'strings';
  if (lower === 'colors.xml' || lower.endsWith('/colors.xml')) return 'colors';
  if (lower === 'dimens.xml' || lower.endsWith('/dimens.xml')) return 'dimens';
  return null;
}

function escapeXmlText(val: string): string {
  return val
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;');
}

function escapeXmlAttr(val: string): string {
  return escapeXmlText(val).replace(/"/g, '&quot;');
}

export function parseAndroidResourcesXml(
  xmlContent: string,
  kind: ResourceKind
): AndroidResourceEntry[] {
  const meta = RESOURCE_FILES_META.find((m) => m.kind === kind)!;
  const tag = meta.tagName;

  try {
    const parser = new DOMParser();
    const doc = parser.parseFromString(xmlContent, 'application/xml');
    if (!doc.querySelector('parsererror')) {
      const nodes = Array.from(doc.getElementsByTagName(tag));
      return nodes.map((el, idx) => ({
        id: `${kind}-${idx}-${el.getAttribute('name') || idx}`,
        name: el.getAttribute('name') || '',
        value: el.textContent || '',
      }));
    }
  } catch {
    // Fallback to regex parsing below
  }

  const regex = new RegExp(`<${tag}\\s+name="([^"]*)"\\s*>([\\s\\S]*?)<\\/${tag}>`, 'g');
  const results: AndroidResourceEntry[] = [];
  let match: RegExpExecArray | null;
  let idx = 0;
  while ((match = regex.exec(xmlContent)) !== null) {
    results.push({
      id: `${kind}-${idx}-${match[1]}`,
      name: match[1],
      value: match[2],
    });
    idx += 1;
  }
  return results;
}

export function serializeAndroidResourcesXml(
  entries: AndroidResourceEntry[],
  kind: ResourceKind
): string {
  const meta = RESOURCE_FILES_META.find((m) => m.kind === kind)!;
  const tag = meta.tagName;
  const lines = entries.map(
    (entry) => `    <${tag} name="${escapeXmlAttr(entry.name)}">${escapeXmlText(entry.value)}</${tag}>`
  );
  return `<?xml version="1.0" encoding="utf-8"?>\n<resources>\n${lines.join('\n')}\n</resources>\n`;
}

export function toHexColorPickerValue(raw: string): string {
  const trimmed = raw.trim();
  if (/^#[0-9A-Fa-f]{6}$/.test(trimmed)) return trimmed;
  // Handle #AARRGGBB -> #RRGGBB
  if (/^#[0-9A-Fa-f]{8}$/.test(trimmed)) return `#${trimmed.slice(3)}`;
  return '#10B981';
}
