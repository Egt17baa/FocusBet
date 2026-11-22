// Puente entre la app y la web de cita previa.
// Solo lee la página y rellena/pulsa lo que la app le pide. No toca captchas ni confirma citas.
(function () {
  if (window.CH) return;

  function norm(s) {
    return String(s || '')
      .normalize('NFD').replace(/[̀-ͯ]/g, '')
      .toUpperCase()
      .replace(/-/g, '')
      .replace(/\s+/g, ' ')
      .trim();
  }

  function find(id) {
    return document.getElementById(id) || document.getElementsByName(id)[0] || null;
  }

  function fire(el, type) {
    el.dispatchEvent(new Event(type, { bubbles: true }));
  }

  var ops = {
    setValue: function (id, value) {
      var el = find(id);
      if (!el) return false;
      el.focus();
      el.value = value;
      fire(el, 'input');
      fire(el, 'change');
      return true;
    },
    check: function (id) {
      var el = find(id);
      if (!el) return false;
      if (!el.checked) el.click();
      if (!el.checked) { el.checked = true; fire(el, 'change'); }
      return true;
    },
    click: function (id) {
      var el = find(id);
      if (!el) return false;
      el.click();
      return true;
    },
    selectByText: function (id, needle) {
      var el = find(id);
      if (!el || !el.options) return false;
      var want = norm(needle);
      var opts = Array.prototype.slice.call(el.options);
      var hit = opts.find(function (o) { return norm(o.text) === want; }) ||
                opts.find(function (o) { return norm(o.text).indexOf(want) !== -1; });
      if (!hit) return false;
      if (el.value === hit.value) return true; // ya elegido: no disparar change (la web recarga la página)
      el.value = hit.value;
      fire(el, 'change');
      return true;
    },
    // Pulsa por id; si no existe, el primer botón/enlace cuyo texto contenga `needle`.
    clickAny: function (id, needle) {
      var el = find(id);
      if (!el) {
        var want = norm(needle);
        var cands = document.querySelectorAll('button, input[type=button], input[type=submit], a');
        el = Array.prototype.find.call(cands, function (c) {
          return norm(c.innerText || c.value || '').indexOf(want) !== -1;
        }) || null;
      }
      if (!el) return false;
      el.click();
      return true;
    },
    // Desplegable que la web rellena sola (p. ej. subtrámite): si tiene varias opciones reales y
    // ninguna elegida, falla para que la persona elija; si solo hay una, la elige.
    needChoice: function (id) {
      var el = find(id);
      if (!el || !el.options) return true;
      var real = Array.prototype.filter.call(el.options, function (o) {
        return o.value !== '' && o.value !== '-1' && o.value !== '0';
      });
      var chosen = el.selectedIndex >= 0 && real.indexOf(el.options[el.selectedIndex]) !== -1;
      if (chosen || real.length === 0) return true;
      if (real.length === 1) { el.value = real[0].value; fire(el, 'change'); return true; }
      return false;
    },
    trySelectByText: function (id, needle) {
      ops.selectByText(id, needle);
      return true;
    }
  };

  window.CH = {
    snapshot: function () {
      var ids = [];
      document.querySelectorAll('[id],[name]').forEach(function (el) {
        if (el.id) ids.push(el.id);
        if (el.name && typeof el.name === 'string') ids.push(el.name);
      });
      var img = document.querySelector('img.img-thumbnail');
      if (img && img.offsetParent !== null) ids.push('__captchaimg');
      var text = document.body ? document.body.innerText : '';
      return JSON.stringify({ url: location.href, ids: ids, text: text.slice(0, 8000) });
    },
    // steps: [[op, id, arg?], ...]. Se detiene en el primer fallo.
    run: function (steps) {
      for (var i = 0; i < steps.length; i++) {
        var s = steps[i];
        var fn = ops[s[0]];
        if (!fn || !fn(s[1], s[2])) return 'missing:' + s[1];
      }
      return 'ok';
    },
    debug: function () {
      var html = document.documentElement ? document.documentElement.outerHTML : '';
      return JSON.stringify({
        href: location.href, title: document.title, ready: document.readyState,
        htmlLen: html.length, frames: window.frames.length,
        scripts: document.scripts.length,
        cookies: (function () { try { return document.cookie.length; } catch (e) { return 'bloqueadas'; } })(),
        head: html.slice(0, 400)
      });
    },
    controls: function () {
      var out = [];
      document.querySelectorAll('input, select, button, textarea').forEach(function (el) {
        if (el.type === 'hidden') return;
        var label = el.tagName.toLowerCase() + (el.id ? '#' + el.id : '') + (el.name && el.name !== el.id ? '[' + el.name + ']' : '');
        var v = el.tagName === 'SELECT'
          ? (el.options.length + ' opciones, elegida: ' + (el.selectedIndex >= 0 ? el.options[el.selectedIndex].text : ''))
          : (el.value || el.innerText || '');
        out.push(label + '="' + String(v).replace(/\s+/g, ' ').trim().slice(0, 40) + '"');
      });
      return out.join('; ').slice(0, 2000);
    },
    options: function (id) {
      var el = find(id);
      if (!el || !el.options) return '[]';
      return JSON.stringify(Array.prototype.map.call(el.options, function (o) { return o.text; }));
    }
  };
})();
