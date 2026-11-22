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
      el.value = hit.value;
      fire(el, 'change');
      return true;
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
    options: function (id) {
      var el = find(id);
      if (!el || !el.options) return '[]';
      return JSON.stringify(Array.prototype.map.call(el.options, function (o) { return o.text; }));
    }
  };
})();
