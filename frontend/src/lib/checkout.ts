export type PaymentMethod = "DEBIT" | "CREDIT" | "TRANSFER";

export type Product = {
  id: string;
  name: string;
  category: string;
  unitPrice: number;
  productDiscountPercent: number;
};

export type CartItem = Product & { quantity: number };
export type Discount = { code: string; name: string; type: string; amount: number; calculationBase: number };
export type Quote = {
  quoteId: string;
  subtotal: number;
  discounts: Discount[];
  shippingCost: number;
  total: number;
  expiresAt: string;
  paymentMethod: PaymentMethod;
};
export type Order = { orderId: string; status: string; confirmedAt: string; quote: Quote };

export const apiBaseUrl = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080/api/v1";
export const money = new Intl.NumberFormat("es-CL", { style: "currency", currency: "CLP", maximumFractionDigits: 0 });
export const categoryIcons: Record<string, string> = {
  Tecnología: "▣",
  Hogar: "▤",
  Despensa: "◒",
  Electrodomésticos: "◫",
  Deportes: "◇"
};
export const paymentCopy: Record<PaymentMethod, { title: string; description: string; badge?: string }> = {
  DEBIT: { title: "Débito", description: "10% de descuento hasta $50.000", badge: "Mejor beneficio" },
  CREDIT: { title: "Crédito", description: "Paga en cuotas según tu banco" },
  TRANSFER: { title: "Transferencia", description: "Confirmación simulada inmediata" }
};

export async function requestQuote(items: CartItem[], paymentMethod: PaymentMethod, signal?: AbortSignal): Promise<Quote> {
  const response = await fetch(`${apiBaseUrl}/checkouts/quote`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ items: items.map(({ id, quantity }) => ({ productId: id, quantity })), paymentMethod }),
    signal
  });
  if (!response.ok) {
    throw new Error((await response.json() as { message?: string }).message ?? "No pude recalcular el checkout.");
  }
  return response.json() as Promise<Quote>;
}
