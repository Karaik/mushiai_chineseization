/**
 * 解析 CSV 字符串
 * @param {string} csvText - CSV 文本内容
 * @returns {Map} - 键值对映射
 */
export function parseCSV(csvText) {
  const rows = csvText.split(/\r?\n/).map(line => line.trim()).filter(line => line);
  const dataMap = new Map();

  for (let i = 1; i < rows.length; i++) {
    const match = rows[i].match(/([^,]+),([^,]*),([^,]*),(.*)/);
    if (match) {
      const key = match[1].trim();
      let content = match[4].trim();
      if (content.startsWith('"') && content.endsWith('"')) {
        content = content.slice(1, -1);
      }
      dataMap.set(key, {
        avatar: match[2].trim(),
        ep: match[3].trim(),
        content
      });
    }
  }

  return dataMap;
}
