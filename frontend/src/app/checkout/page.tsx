import type { Metadata } from "next";
import { PaymentPage } from "../../components/PaymentPage";

export const metadata: Metadata = { title: "Pago | Walmart Demo" };

export default function Page() {
  return <PaymentPage />;
}
