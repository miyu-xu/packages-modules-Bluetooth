LINE 914:       /* See if we can send anything from the Link Queue */
LINE 915:       if (p_lcb->link_xmit_data_q != NULL && !list_is_empty(p_lcb->link_xmit_data_q)) {
LINE 916:         log::verbose("Sending to lower layer");
LINE 917:         p_buf = (BT_HDR*)list_front(p_lcb->link_xmit_data_q);
LINE 918:         list_remove(p_lcb->link_xmit_data_q, p_buf);
LINE 919:         l2c_link_send_to_lower(p_lcb, p_buf, NULL);
LINE 920:       } else if (single_write) {
LINE 921:         /* If only doing one write, break out */
LINE 922:         log::debug("single_write is true, skipping");
LINE 923:         break;
LINE 924:       } else {
LINE 925:         /* If nothing on the link queue, check the channel queue */
LINE 926:         tL2C_TX_COMPLETE_CB_INFO cbi = {};
LINE 927:         log::debug("Check next buffer");
LINE 928:         p_buf = l2cu_get_next_buffer_to_send(p_lcb, &cbi);
LINE 929:         if (p_buf != NULL) {
LINE 930:           log::debug("Sending next buffer");
LINE 931:           l2c_link_send_to_lower(p_lcb, p_buf, &cbi);
LINE 932:         }
LINE 933:       }
