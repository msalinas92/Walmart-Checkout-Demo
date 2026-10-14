const chromeEndpoint = process.env.CHROME_ENDPOINT ?? "http://127.0.0.1:9223";
const webUrl = process.env.WEB_URL ?? "https://retail-checkout-web-iva4iqx3eq-tl.a.run.app";
const target = await fetch(`${chromeEndpoint}/json/new?about:blank`, { method: "PUT" }).then((response) => response.json());
const socket = new WebSocket(target.webSocketDebuggerUrl);
let requestId = 0;
const pending = new Map();

socket.addEventListener("message", (event) => {
  const message = JSON.parse(event.data);
  if (!message.id || !pending.has(message.id)) return;
  const { resolve, reject } = pending.get(message.id);
  pending.delete(message.id);
  if (message.error) reject(new Error(message.error.message));
  else resolve(message.result);
});

await new Promise((resolve, reject) => {
  socket.addEventListener("open", resolve, { once: true });
  socket.addEventListener("error", reject, { once: true });
});

function command(method, params = {}) {
  return new Promise((resolve, reject) => {
    const id = ++requestId;
    pending.set(id, { resolve, reject });
    socket.send(JSON.stringify({ id, method, params }));
  });
}

async function evaluate(expression) {
  const result = await command("Runtime.evaluate", { expression, awaitPromise: true, returnByValue: true });
  if (result.exceptionDetails) throw new Error(result.exceptionDetails.text);
  return result.result.value;
}

async function waitFor(expression, description, timeout = 15000) {
  return evaluate(`new Promise((resolve, reject) => {
    const startedAt = Date.now();
    const timer = setInterval(() => {
      if (${expression}) { clearInterval(timer); resolve(true); }
      else if (Date.now() - startedAt > ${timeout}) { clearInterval(timer); reject(new Error(${JSON.stringify(description)})); }
    }, 200);
  })`);
}

await command("Runtime.enable");
await command("Page.enable");
await command("Page.navigate", { url: webUrl });
await new Promise((resolve) => setTimeout(resolve, 1200));
await waitFor("document.querySelectorAll('.catalog-product').length === 12", "El catálogo no cargó 12 productos");

await evaluate("document.querySelector('.menu').click()");
await new Promise((resolve) => setTimeout(resolve, 200));
const home = await evaluate(`({
  route: location.pathname,
  title: document.title,
  menuOpen: document.querySelector('.drawer').classList.contains('open'),
  menuLinks: [...document.querySelectorAll('.drawer > a')].map((link) => link.textContent.trim()),
  categories: [...document.querySelectorAll('.categories button')].map((button) => button.textContent.trim()),
  productCount: document.querySelectorAll('.catalog-product').length
})`);

await evaluate(`(() => {
  document.querySelector('.drawer-heading button').click();
  [...document.querySelectorAll('.categories button')].find((button) => button.textContent.trim() === 'Tecnología').click();
})()`);
await waitFor("document.querySelectorAll('.catalog-product').length === 4", "El filtro Tecnología no devolvió cuatro productos");
const technology = await evaluate("[...document.querySelectorAll('.catalog-product > p')].map((item) => item.textContent.trim())");

await evaluate(`(() => {
  [...document.querySelectorAll('.categories button')].find((button) => button.textContent.trim() === 'Todos').click();
  const input = document.querySelector('.search input');
  Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value').set.call(input, 'café');
  input.dispatchEvent(new Event('input', { bubbles: true }));
})()`);
await waitFor("document.querySelectorAll('.catalog-product').length === 1", "La búsqueda café no devolvió un producto");
const searchResults = await evaluate("[...document.querySelectorAll('.catalog-product h3')].map((item) => item.textContent.trim())");

await evaluate(`(() => {
  const input = document.querySelector('.search input');
  Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value').set.call(input, '');
  input.dispatchEvent(new Event('input', { bubbles: true }));
})()`);
await waitFor("document.querySelectorAll('.catalog-product').length === 12", "El catálogo no se restauró después de buscar");
await evaluate(`(() => {
  const product = [...document.querySelectorAll('.catalog-product')].find((item) => item.querySelector('h3').textContent === 'Smart TV 55 pulgadas');
  product.querySelector('button.primary').click();
})()`);
await waitFor("document.querySelector('.cart-dock') && document.querySelector('.bag span').textContent === '1'", "El producto no llegó al carrito");
const selected = await evaluate(`({ dock: document.querySelector('.cart-dock b').textContent, cartCount: document.querySelector('.bag span').textContent })`);

await evaluate("document.querySelector('.cart-dock a').click()");
await new Promise((resolve) => setTimeout(resolve, 1200));
await waitFor("location.pathname === '/carrito' && document.querySelector('.products .product')", "No navegó a la página de carrito");
await waitFor("document.querySelector('.summary .total strong').textContent !== '$0'", "El carrito no recibió su cotización");
const cart = await evaluate(`({
  route: location.pathname,
  heading: document.querySelector('.page-heading h1').textContent,
  products: document.querySelectorAll('.products .product').length,
  total: document.querySelector('.summary .total strong').textContent,
  nextHref: document.querySelector('.summary .primary-link').getAttribute('href')
})`);

await evaluate("document.querySelector('.summary .primary-link').click()");
await new Promise((resolve) => setTimeout(resolve, 1200));
await waitFor("location.pathname === '/checkout' && document.querySelectorAll('.payment-option').length === 3", "No navegó a la página de pago");
await waitFor("document.querySelector('.summary button.primary') && !document.querySelector('.summary button.primary').disabled", "El checkout no terminó de cotizar");
const payment = await evaluate(`({
  route: location.pathname,
  heading: document.querySelector('.page-heading h1').textContent,
  methods: [...document.querySelectorAll('.payment-option b')].map((item) => item.textContent),
  total: document.querySelector('.summary .total strong').textContent
})`);

await evaluate("document.querySelector('.summary button.primary').click()");
await waitFor("document.querySelector('.confirmation-card')", "La compra no llegó a la confirmación", 20000);
const confirmation = await evaluate(`({
  route: location.pathname,
  heading: document.querySelector('.confirmation-card h1').textContent,
  orderId: document.querySelector('.confirmation-number code').textContent,
  eventFlow: document.querySelector('.event-proof').textContent.trim()
})`);

const checks = {
  homeRoute: home.route === "/",
  walmartTitle: home.title.startsWith("Walmart Demo"),
  menuOpens: home.menuOpen && home.menuLinks.length === 3,
  fiveCategories: home.categories.length === 6,
  twelveProducts: home.productCount === 12,
  categoryFilter: technology.length === 4 && technology.every((item) => item === "Tecnología"),
  search: searchResults.length === 1 && searchResults[0] === "Pack café premium",
  cartPersistence: selected.cartCount === "1" && cart.route === "/carrito" && cart.products === 1,
  separateCheckout: cart.nextHref === "/checkout" && payment.route === "/checkout" && payment.methods.length === 3,
  confirmation: confirmation.heading === "¡Todo listo!" && confirmation.orderId.startsWith("ord_")
};

console.log(JSON.stringify({ home, technology, searchResults, selected, cart, payment, confirmation, checks }, null, 2));
socket.close();
if (Object.values(checks).some((passed) => !passed)) process.exit(1);
